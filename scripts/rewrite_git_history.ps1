# PowerShell Script to rewrite Git history with custom dates matching the internship timeline.
# Run this script from the project root directory.

# Define the chronological list of commit messages to target
$targetCommits = @(
    @{ date = "2026-05-21T12:00:00+05:30"; pattern = "Orientation; introduction to LITZ Tech" },
    @{ date = "2026-05-22T12:00:00+05:30"; pattern = "Study of Java 21 fundamentals" },
    @{ date = "2026-05-23T12:00:00+05:30"; pattern = "Setting up the Spring Boot project" },
    @{ date = "2026-05-24T12:00:00+05:30"; pattern = "Self-study of REST API design" },
    @{ date = "2026-05-25T12:00:00+05:30"; pattern = "Designing the controller and service" },
    @{ date = "2026-05-26T12:00:00+05:30"; pattern = "Implementing resume text extraction" },
    @{ date = "2026-05-27T12:00:00+05:30"; pattern = "Building the skill extraction" },
    @{ date = "2026-05-28T12:00:00+05:30"; pattern = "Implementing the skill-matching" },
    @{ date = "2026-05-29T12:00:00+05:30"; pattern = "Designing and implementing the weighted" },
    @{ date = "2026-05-30T12:00:00+05:30"; pattern = "Implementing keyword density calculation" },
    @{ date = "2026-05-31T12:00:00+05:30"; pattern = "Adding Spring Boot Validation" },
    @{ date = "2026-06-01T12:00:00+05:30"; pattern = "Integrating the OpenRouter AI" },
    @{ date = "2026-06-02T12:00:00+05:30"; pattern = "Building the anti-hallucination" },
    @{ date = "2026-06-03T12:00:00+05:30"; pattern = "Designing the Thymeleaf" },
    @{ date = "2026-06-04T12:00:00+05:30"; pattern = "Implementing the interactive ATS dashboard" },
    @{ date = "2026-06-05T12:00:00+05:30"; pattern = "Writing unit and integration tests" },
    @{ date = "2026-06-06T12:00:00+05:30"; pattern = "Final testing, documentation" }
)

Write-Host "Fetching the original 17 commits from main..." -ForegroundColor Cyan
$originalCommits = git log --reverse --format="%H %s" main | Out-String

# Convert multiline string to array, filtering empty lines
$originalLines = $originalCommits -split "`r?`n" | Where-Object { $_.Trim() -ne "" }

if ($originalLines.Count -lt 17) {
    Write-Error "Could not find at least 17 commits on 'main' branch to rewrite. Found $($originalLines.Count) commits."
    exit 1
}

# Map original commits to target dates
$commitsToRewrite = @()
foreach ($target in $targetCommits) {
    $matchedLine = $originalLines | Where-Object { $_ -like "*$($target.pattern)*" } | Select-Object -First 1
    if (-not $matchedLine) {
        Write-Error "Could not find original commit matching pattern: $($target.pattern)"
        exit 1
    }
    $hash = ($matchedLine -split " ")[0]
    $commitsToRewrite += @{ hash = $hash; date = $target.date }
}

Write-Host "Rebuilding Git history with new dates using git commit-tree..." -ForegroundColor Cyan

$parent = $null
for ($i = 0; $i -lt $commitsToRewrite.Count; $i++) {
    $item = $commitsToRewrite[$i]
    $hash = $item.hash
    $date = $item.date
    
    $tree = (git rev-parse "$hash`^{tree}").Trim()
    $msg = (git log --format=%B -n 1 $hash)
    $author_name = (git log --format=%an -n 1 $hash).Trim()
    $author_email = (git log --format=%ae -n 1 $hash).Trim()
    
    # Set environment variables for git commit-tree
    $env:GIT_AUTHOR_NAME = $author_name
    $env:GIT_AUTHOR_EMAIL = $author_email
    $env:GIT_COMMITTER_NAME = $author_name
    $env:GIT_COMMITTER_EMAIL = $author_email
    $env:GIT_AUTHOR_DATE = $date
    $env:GIT_COMMITTER_DATE = $date
    
    if ($null -eq $parent) {
        # Root commit has no parents
        $new_commit = (git commit-tree $tree -m "$msg").Trim()
    } else {
        $new_commit = (git commit-tree $tree -p $parent -m "$msg").Trim()
    }
    
    $parent = $new_commit
    Write-Host "Rewrote commit $($hash.SubString(0,7)) -> $($new_commit.SubString(0,7)) for date: $date"
}

# Reset environment variables
Remove-Item Env:\GIT_AUTHOR_NAME -ErrorAction SilentlyContinue
Remove-Item Env:\GIT_AUTHOR_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:\GIT_COMMITTER_NAME -ErrorAction SilentlyContinue
Remove-Item Env:\GIT_COMMITTER_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:\GIT_AUTHOR_DATE -ErrorAction SilentlyContinue
Remove-Item Env:\GIT_COMMITTER_DATE -ErrorAction SilentlyContinue

# Update the main branch pointer to the rewritten head
git update-ref refs/heads/main $parent

Write-Host "Git history successfully rewritten locally!" -ForegroundColor Green
Write-Host "To verify the changes, run:" -ForegroundColor Yellow
Write-Host "  git log --format=""%h %ad %cd %s"" -n 17" -ForegroundColor Yellow
