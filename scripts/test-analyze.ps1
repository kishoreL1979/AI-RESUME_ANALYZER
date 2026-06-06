param(
    [string]$FilePath = "",
    [int]$Port = 8086,
    [string]$JobDescription = "Java Spring Boot AWS"
)

function Write-Log($msg) { Write-Host "[test-analyze] $msg" }

if (-not $FilePath) {
    Write-Log "No file provided - creating a minimal DOCX sample for testing."
    $tmp = Join-Path $PWD "temp_docx"
    if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
    New-Item -ItemType Directory -Path $tmp | Out-Null

    $relsDir = Join-Path $tmp "_rels"
    $wordDir = Join-Path $tmp "word"
    New-Item -ItemType Directory -Path $relsDir | Out-Null
    New-Item -ItemType Directory -Path $wordDir | Out-Null

    $contentTypesLines = @(
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">',
        '  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>',
        '  <Default Extension="xml" ContentType="application/xml"/>',
        '  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>',
        '</Types>'
    )
    $contentTypesLines | Out-File -LiteralPath (Join-Path $tmp "[Content_Types].xml") -Encoding UTF8

    $relsLines = @(
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">',
        '  <Relationship Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml" Id="rId1"/>',
        '</Relationships>'
    )
    $relsLines | Out-File -LiteralPath (Join-Path $relsDir ".rels") -Encoding UTF8

    $docLines = @(
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">',
        '  <w:body>',
        '    <w:p><w:r><w:t>Sample resume: Java, Spring Boot, AWS. 5 years experience.</w:t></w:r></w:p>',
        '    <w:sectPr/>',
        '  </w:body>',
        '</w:document>'
    )
    $docLines | Out-File -LiteralPath (Join-Path $wordDir "document.xml") -Encoding UTF8

    $timestamp = Get-Date -Format "yyyyMMddHHmmss"
    $zipPath = Join-Path $PWD ("sample_resume_{0}.zip" -f $timestamp)
    if (Test-Path $zipPath) { Remove-Item -Force -ErrorAction SilentlyContinue $zipPath }
    Compress-Archive -Path (Join-Path $tmp "*") -DestinationPath $zipPath -Force
    $timestamp = Get-Date -Format "yyyyMMddHHmmss"
    $docxPath = Join-Path $PWD ("sample_resume_{0}.docx" -f $timestamp)
    if (Test-Path $docxPath) { Remove-Item -Force -ErrorAction SilentlyContinue $docxPath }
    Move-Item -Path $zipPath -Destination $docxPath -Force
    Remove-Item -Recurse -Force $tmp
    $FilePath = $docxPath
    Write-Log "Created sample DOCX at $FilePath"
}

if (-not (Test-Path $FilePath)) { Write-Log "File not found: $FilePath"; exit 2 }

$url = "http://localhost:$Port/api/analyze"
Write-Log "Posting to $url with file $FilePath and job description: $JobDescription"

$fileItem = Get-Item -Path $FilePath

$form = @{ file = $fileItem; jobDescription = $JobDescription }

try {
    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    $multipart = New-Object System.Net.Http.MultipartFormDataContent

    $fileStream = [System.IO.File]::OpenRead($FilePath)
    $fileContent = New-Object System.Net.Http.StreamContent($fileStream)
    $fileName = [System.IO.Path]::GetFileName($FilePath)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    $multipart.Add($fileContent, "file", $fileName)
    $multipart.Add((New-Object System.Net.Http.StringContent($JobDescription)), "jobDescription")

    $respTask = $client.PostAsync($url, $multipart)
    $respTask.Wait()
    $resp = $respTask.Result
    $respStr = $resp.Content.ReadAsStringAsync().Result
    $fileStream.Close()
    $client.Dispose()
    if (-not $resp.IsSuccessStatusCode) {
        Write-Log "Request failed - HTTP $($resp.StatusCode): $respStr"
        exit 3
    }
    $response = $respStr | ConvertFrom-Json -ErrorAction Stop
} catch {
    Write-Log "Request failed: $_"
    exit 3
}

Write-Log "Response received:`n"
Write-Output ($response | ConvertTo-Json -Depth 5)

# Validate required fields
$required = @('atsScore','matchPercentage','matchedSkills','missingSkills','strengths','weaknesses','recommendations','resumeSummary')
$missing = @()
foreach ($k in $required) {
    if (-not $response.PSObject.Properties.Name -contains $k) { $missing += $k }
}

if ($missing.Count -gt 0) {
    Write-Log "Validation failed - missing keys: $($missing -join ', ')"
    exit 4
} else {
    Write-Log "Validation passed - all required keys present."
    exit 0
}
