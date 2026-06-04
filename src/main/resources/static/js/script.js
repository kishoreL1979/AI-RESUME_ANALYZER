/**
 * AI Resume Analyzer — Premium Frontend v2
 */

(function () {
  "use strict";

  let chartInstances = {};
  let lastAnalysisData = null;

  const ANALYSIS_STEPS = [
    "Extracting Resume Content",
    "Analyzing Skills",
    "Calculating ATS Score",
    "Matching Job Description",
    "Generating AI Insights",
    "Preparing Report",
  ];

  function $(selector, parent = document) {
    return parent.querySelector(selector);
  }

  function $$(selector, parent = document) {
    return Array.from(parent.querySelectorAll(selector));
  }

  function formatFileSize(bytes) {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  }

  function animateCounter(element, target, duration = 1500, suffix = "") {
    const startTime = performance.now();
    function update(currentTime) {
      const progress = Math.min((currentTime - startTime) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      element.textContent = Math.round(target * eased) + suffix;
      if (progress < 1) requestAnimationFrame(update);
    }
    requestAnimationFrame(update);
  }

  function getScoreGrade(score) {
    if (score >= 85) return { label: "Excellent", class: "excellent" };
    if (score >= 70) return { label: "Good", class: "good" };
    if (score >= 50) return { label: "Fair", class: "fair" };
    return { label: "Needs Work", class: "poor" };
  }

  function getChartColors() {
    const isDark = document.documentElement.getAttribute("data-theme") === "dark";
    return {
      text: isDark ? "#94a3b8" : "#64748b",
      grid: isDark ? "rgba(100, 116, 139, 0.2)" : "rgba(148, 163, 184, 0.15)",
      bg: isDark ? "#151d2e" : "#ffffff",
    };
  }

  /* ---------- Theme ---------- */
  function initTheme() {
    const saved = localStorage.getItem("theme") || "light";
    applyTheme(saved);

    const toggle = $("#themeToggle");
    if (toggle) {
      toggle.addEventListener("click", () => {
        const current = document.documentElement.getAttribute("data-theme");
        applyTheme(current === "dark" ? "light" : "dark");
      });
    }
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("theme", theme);
    if (lastAnalysisData) updateCharts(lastAnalysisData);
  }

  /* ---------- Navbar ---------- */
  function initNavbar() {
    const navbar = $(".navbar-premium");
    if (!navbar) return;
    window.addEventListener("scroll", () => {
      navbar.classList.toggle("scrolled", window.scrollY > 20);
    });
  }

  /* ---------- Scroll Reveal ---------- */
  function initScrollReveal() {
    const sections = $$(".fade-in-section:not(.visible)");
    if (!sections.length) return;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("visible");
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -30px 0px" }
    );
    sections.forEach((s) => observer.observe(s));
  }

  /* ---------- Landing Counters ---------- */
  function initLandingCounters() {
    const counters = $$("[data-counter]");
    if (!counters.length) return;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            animateCounter(entry.target, parseInt(entry.target.dataset.counter, 10));
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.5 }
    );
    counters.forEach((c) => observer.observe(c));
  }

  function initTooltips() {
    $$('[data-bs-toggle="tooltip"]').forEach((el) => new bootstrap.Tooltip(el));
  }

  /* ---------- File Upload ---------- */
  function initFileUpload() {
    const uploadZone = $("#uploadZone");
    const fileInput = $("#resumeFile");
    const filePreview = $("#filePreview");
    const fileName = $("#fileName");
    const fileSize = $("#fileSize");
    const removeBtn = $("#removeFile");
    if (!uploadZone || !fileInput) return;

    const allowedExtensions = [".pdf", ".docx"];
    const allowedTypes = [
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ];

    function isValidFile(file) {
      const ext = file.name.substring(file.name.lastIndexOf(".")).toLowerCase();
      return allowedTypes.includes(file.type) || allowedExtensions.includes(ext);
    }

    function showFilePreview(file) {
      fileName.textContent = file.name;
      fileSize.textContent = formatFileSize(file.size);
      filePreview.classList.add("visible");
      uploadZone.style.display = "none";
    }

    function clearFile() {
      fileInput.value = "";
      filePreview.classList.remove("visible");
      uploadZone.style.display = "block";
    }

    uploadZone.addEventListener("click", () => fileInput.click());
    uploadZone.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        fileInput.click();
      }
    });
    uploadZone.addEventListener("dragover", (e) => { e.preventDefault(); uploadZone.classList.add("dragover"); });
    uploadZone.addEventListener("dragleave", () => uploadZone.classList.remove("dragover"));
    uploadZone.addEventListener("drop", (e) => {
      e.preventDefault();
      uploadZone.classList.remove("dragover");
      const file = e.dataTransfer.files[0];
      if (file && isValidFile(file)) {
        const dt = new DataTransfer();
        dt.items.add(file);
        fileInput.files = dt.files;
        showFilePreview(file);
      } else {
        showToast("Please upload a PDF or DOCX file.", "warning");
      }
    });
    fileInput.addEventListener("change", () => {
      const file = fileInput.files[0];
      if (file) {
        if (isValidFile(file)) showFilePreview(file);
        else { clearFile(); showToast("Please upload a PDF or DOCX file.", "warning"); }
      }
    });
    if (removeBtn) removeBtn.addEventListener("click", (e) => { e.stopPropagation(); clearFile(); });
  }

  /* ---------- Char Counter ---------- */
  function initCharCounter() {
    const textarea = $("#jobDescription");
    const counter = $("#charCount");
    const maxChars = 5000;
    if (!textarea || !counter) return;

    textarea.addEventListener("input", () => {
      let length = textarea.value.length;
      if (length >= maxChars) {
        textarea.value = textarea.value.substring(0, maxChars);
        length = maxChars;
      }
      counter.textContent = length.toLocaleString();
      const wrap = counter.closest(".char-counter");
      wrap.classList.remove("warning", "danger");
      if (length > maxChars * 0.9) wrap.classList.add("warning");
      if (length >= maxChars) wrap.classList.add("danger");
    });
  }

  /* ---------- Mock Data ---------- */
  function getMockAnalysisResult() {
    return {
      atsScore: 87,
      resumeQuality: 82,
      skillMatch: 78,
      keywordDensity: 72,
      analysisConfidence: 91,
      matchedSkills: [
        "JavaScript", "React", "Node.js", "TypeScript", "REST APIs",
        "Git", "Agile", "SQL", "Problem Solving", "Team Leadership", "CI/CD", "AWS",
      ],
      missingSkills: [
        { name: "Kubernetes", critical: true },
        { name: "GraphQL", critical: true },
        { name: "Docker", critical: true },
        { name: "Microservices", critical: false },
        { name: "System Design", critical: false },
        { name: "Redis", critical: false },
      ],
      summary:
        "Your resume demonstrates strong full-stack development experience with a solid foundation in modern JavaScript frameworks. The professional summary effectively highlights your technical expertise, though quantifiable achievements could be strengthened. Work experience sections are well-structured with relevant keywords, making your profile competitive for senior developer roles.",
      resumePreview: {
        name: "Alex Morgan",
        title: "Senior Full-Stack Developer",
        education: [
          { degree: "B.S. Computer Science", school: "Stanford University", year: "2018" },
          { degree: "M.S. Software Engineering", school: "Georgia Tech", year: "2020" },
        ],
        skills: ["JavaScript", "React", "Node.js", "TypeScript", "AWS", "PostgreSQL", "Docker", "GraphQL"],
        projects: [
          { name: "E-Commerce Platform", desc: "Built scalable microservices architecture serving 50K+ daily users with 99.9% uptime." },
          { name: "AI Dashboard", desc: "Developed real-time analytics dashboard reducing reporting time by 60%." },
        ],
        experience: [
          { role: "Senior Software Engineer", company: "TechCorp Inc.", period: "2021 — Present", desc: "Led development of customer-facing applications using React and Node.js. Reduced page load times by 40%." },
          { role: "Full-Stack Developer", company: "StartupXYZ", period: "2019 — 2021", desc: "Built REST APIs and frontend components for SaaS platform with 10K+ active users." },
        ],
      },
      analytics: { formatting: "92%", keywords: "78%", experience: "85%", impact: "74%" },
      radar: { formatting: 92, keywords: 78, experience: 85, skills: 88, education: 80, impact: 74 },
      atsBreakdown: { keywordMatch: 85, formatting: 92, experience: 78, education: 88, skills: 82 },
      suggestions: [
        { icon: "bi-lightbulb", title: "Add Quantifiable Metrics", text: "Include specific numbers like 'Reduced load time by 40%' or 'Led team of 8 developers' to demonstrate impact." },
        { icon: "bi-key", title: "Optimize Keywords", text: "Incorporate missing keywords like Kubernetes and Docker naturally within your experience descriptions." },
        { icon: "bi-layout-text-window", title: "Improve Formatting", text: "Use consistent bullet points and ensure section headers match standard ATS parsing patterns." },
        { icon: "bi-star", title: "Strengthen Summary", text: "Lead with your most relevant skills and years of experience tailored to the target job description." },
      ],
      strengths: [
        "Strong technical skill alignment with job requirements",
        "Clear and professional resume structure",
        "Relevant work experience with industry keywords",
        "Well-organized skills section with modern technologies",
      ],
      weaknesses: [
        "Missing several key technologies mentioned in the job description",
        "Limited quantifiable achievements in work experience",
        "Professional summary could be more targeted",
        "Some sections lack industry-specific terminology",
      ],
      keywords: [
        { keyword: "JavaScript", count: 8, density: 92 },
        { keyword: "React", count: 6, density: 85 },
        { keyword: "Node.js", count: 5, density: 78 },
        { keyword: "API", count: 4, density: 65 },
        { keyword: "Git", count: 3, density: 55 },
        { keyword: "Agile", count: 3, density: 50 },
        { keyword: "SQL", count: 2, density: 42 },
        { keyword: "AWS", count: 2, density: 38 },
      ],
    };
  }

  /* ---------- Multi-Step Loading ---------- */
  function runAnalysisSteps() {
    return new Promise((resolve) => {
      const overlay = $("#analysisOverlay");
      const progressFill = $("#analysisProgressFill");
      const progressBar = $("#analysisProgressBar");
      const percentEl = $("#analysisPercent");
      const stepLabel = $("#analysisStepLabel");
      const stepEls = $$(".analysis-step");

      if (!overlay) {
        setTimeout(resolve, 3500);
        return;
      }

      overlay.classList.add("active");
      overlay.setAttribute("aria-hidden", "false");
      document.body.style.overflow = "hidden";

      let currentStep = 0;
      const totalSteps = ANALYSIS_STEPS.length;
      const stepDuration = 550;

      function updateStep() {
        stepEls.forEach((el, i) => {
          el.classList.remove("active", "done");
          if (i < currentStep) {
            el.classList.add("done");
            el.querySelector(".step-indicator").innerHTML = '<i class="bi bi-check"></i>';
          } else if (i === currentStep) {
            el.classList.add("active");
          }
        });

        const pct = Math.round(((currentStep + 1) / totalSteps) * 100);
        progressFill.style.width = pct + "%";
        percentEl.textContent = pct + "%";
        progressBar.setAttribute("aria-valuenow", pct);
        stepLabel.textContent = ANALYSIS_STEPS[currentStep];

        currentStep++;
        if (currentStep < totalSteps) {
          setTimeout(updateStep, stepDuration);
        } else {
          setTimeout(() => {
            overlay.classList.remove("active");
            overlay.setAttribute("aria-hidden", "true");
            document.body.style.overflow = "";
            stepEls.forEach((el) => {
              el.classList.remove("active");
              el.classList.add("done");
              el.querySelector(".step-indicator").innerHTML = '<i class="bi bi-check"></i>';
            });
            resolve();
          }, 400);
        }
      }

      progressFill.style.width = "0%";
      percentEl.textContent = "0%";
      stepEls.forEach((el) => {
        el.classList.remove("active", "done");
        el.querySelector(".step-indicator").textContent = parseInt(el.dataset.step, 10) + 1;
      });
      updateStep();
    });
  }

  /* ---------- Charts ---------- */
  function destroyCharts() {
    Object.values(chartInstances).forEach((c) => c?.destroy());
    chartInstances = {};
  }

  function updateCharts(data) {
    if (typeof Chart === "undefined") return;
    destroyCharts();

    const colors = getChartColors();
    Chart.defaults.font.family = '"Inter", system-ui, sans-serif';
    Chart.defaults.color = colors.text;

    const matched = data.matchedSkills.length;
    const missing = data.missingSkills.length;

    /* Skill Match Pie */
    const pieCtx = $("#skillMatchChart");
    if (pieCtx) {
      chartInstances.pie = new Chart(pieCtx, {
        type: "doughnut",
        data: {
          labels: ["Matched Skills", "Missing Skills"],
          datasets: [{
            data: [matched, missing],
            backgroundColor: ["#10b981", "#ef4444"],
            borderWidth: 0,
            hoverOffset: 8,
          }],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          cutout: "65%",
          plugins: {
            legend: { position: "bottom", labels: { padding: 16, usePointStyle: true, pointStyle: "circle" } },
          },
        },
      });
    }

    /* Percentages for Radar / Breakdown */
    const completenessPct = data.atsBreakdown.completeness ? Math.round((data.atsBreakdown.completeness / 15) * 100) : 0;
    const keywordCoveragePct = data.atsBreakdown.keywordCoverage ? Math.round((data.atsBreakdown.keywordCoverage / 15) * 100) : 0;
    const educationPct = data.atsBreakdown.education ? Math.round((data.atsBreakdown.education / 20) * 100) : 0;
    const skillMatchPct = data.atsBreakdown.skillMatch ? Math.round((data.atsBreakdown.skillMatch / 40) * 100) : 0;
    const projectsCertsPct = data.atsBreakdown.projectsCertifications ? Math.round((data.atsBreakdown.projectsCertifications / 10) * 100) : 0;
    const impactPct = 80; // default baseline fallback

    /* Radar */
    const radarCtx = $("#radarChart");
    if (radarCtx) {
      chartInstances.radar = new Chart(radarCtx, {
        type: "radar",
        data: {
          labels: ["Formatting", "Keywords", "Projects/Certs", "Skills", "Education", "Impact"],
          datasets: [{
            label: "Compatibility Score %",
            data: [completenessPct, keywordCoveragePct, projectsCertsPct, skillMatchPct, educationPct, impactPct],
            backgroundColor: "rgba(79, 70, 229, 0.15)",
            borderColor: "#6366f1",
            borderWidth: 2,
            pointBackgroundColor: "#6366f1",
            pointRadius: 4,
          }],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            r: {
              beginAtZero: true,
              max: 100,
              ticks: { stepSize: 20, backdropColor: "transparent" },
              grid: { color: colors.grid },
              pointLabels: { font: { size: 11, weight: "600" } },
            },
          },
          plugins: { legend: { display: false } },
        },
      });
    }

    /* Keyword Bar */
    const barCtx = $("#keywordBarChart");
    if (barCtx && data.keywordDensity) {
      const topKeywords = Object.entries(data.keywordDensity || {}).map(([kw, val]) => ({
        keyword: kw,
        count: val ? (val.count || 0) : 0,
        density: val ? (val.density || 0) : 0
      })).slice(0, 6);

      chartInstances.bar = new Chart(barCtx, {
        type: "bar",
        data: {
          labels: topKeywords.map((k) => k.keyword),
          datasets: [{
            label: "Coverage %",
            data: topKeywords.map((k) => k.density),
            backgroundColor: topKeywords.map((k) =>
              k.density >= 70 ? "rgba(16,185,129,0.8)" : k.density >= 40 ? "rgba(245,158,11,0.8)" : "rgba(239,68,68,0.8)"
            ),
            borderRadius: 6,
            borderSkipped: false,
          }],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: { beginAtZero: true, max: 100, grid: { color: colors.grid }, ticks: { callback: (v) => v + "%" } },
            x: { grid: { display: false } },
          },
          plugins: { legend: { display: false } },
        },
      });
    }

    /* ATS Breakdown */
    const atsCtx = $("#atsBreakdownChart");
    if (atsCtx) {
      chartInstances.ats = new Chart(atsCtx, {
        type: "bar",
        data: {
          labels: ["Skills Match %", "Education Match %", "Formatting Quality %", "Keywords Coverage %", "Projects/Certs %"],
          datasets: [{
            label: "Category Score %",
            data: [skillMatchPct, educationPct, completenessPct, keywordCoveragePct, projectsCertsPct],
            backgroundColor: [
              "rgba(79,70,229,0.8)",
              "rgba(124,58,237,0.8)",
              "rgba(99,102,241,0.8)",
              "rgba(139,92,246,0.8)",
              "rgba(167,139,250,0.8)",
            ],
            borderRadius: 6,
          }],
        },
        options: {
          indexAxis: "y",
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            x: { beginAtZero: true, max: 100, grid: { color: colors.grid } },
            y: { grid: { display: false } },
          },
          plugins: { legend: { display: false } },
        },
      });
    }
  }

  function getSkillsArray(skills) {
    if (!skills) return [];
    if (Array.isArray(skills)) return skills;
    return [
      ...(skills.programmingLanguages || []),
      ...(skills.frameworks || []),
      ...(skills.libraries || []),
      ...(skills.databases || []),
      ...(skills.cloud || []),
      ...(skills.devops || []),
      ...(skills.tools || [])
    ].filter(Boolean);
  }

  function getEducationString(edu) {
    if (!edu) return "Not Available";
    if (typeof edu === "string") return edu;
    const parts = [];
    if (edu.degree) parts.push(edu.degree);
    if (edu.department) parts.push(edu.department);
    if (edu.college) parts.push(`from ${edu.college}`);
    if (edu.university) parts.push(`under ${edu.university}`);
    if (edu.graduationYear) parts.push(`(${edu.graduationYear})`);
    if (edu.cgpa) parts.push(`(CGPA: ${edu.cgpa})`);
    if (edu.percentage) parts.push(`(Percentage: ${edu.percentage})`);
    return parts.length > 0 ? parts.join(" ") : "Not Available";
  }

  /* ---------- Resume Preview ---------- */
  function renderResumePreview(profile) {
    const container = $("#resumePreviewBody");
    if (!container) return;

    const pi = profile.personalInfo || {};
    if (!profile || (!pi.name && !pi.email && !pi.phone)) {
      container.innerHTML = `<p class="text-secondary-custom small mb-0">Upload and analyze a resume to see the preview.</p>`;
      return;
    }

    const fallbackStr = "Not Available";
    const skillsList = getSkillsArray(profile.skills);
    const educationStr = getEducationString(profile.education);

    const skillsHtml = skillsList.length > 0 ? 
      skillsList.map(s => `<span class="preview-skill"><i class="bi bi-tag-fill me-1"></i>${s}</span>`).join("") : 
      `<span class="text-muted">${fallbackStr}</span>`;

    const educationHtml = educationStr !== "Not Available" ? 
      `<div class="preview-timeline-item">
         <div class="timeline-dot"><i class="bi bi-mortarboard-fill"></i></div>
         <div class="timeline-content">
           <div class="timeline-title">${educationStr}</div>
         </div>
       </div>` : 
      `<div class="text-muted ms-3">${fallbackStr}</div>`;

    const projectsHtml = Array.isArray(profile.projects ?? []) && (profile.projects ?? []).length > 0 ? 
      (profile.projects ?? []).map(p => `
        <div class="project-mini-card p-2 border rounded mb-2 bg-light-subtle">
          <div class="project-mini-title fw-bold text-primary"><i class="bi bi-git me-1"></i>${p.projectName || ""} ${p.role ? `(${p.role})` : ""}</div>
          <div class="project-mini-desc small text-secondary">${p.description || ""}</div>
          ${p.techStack && p.techStack.length > 0 ? `<div class="project-mini-tech mt-1 small text-muted"><strong>Tech:</strong> ${p.techStack.join(", ")}</div>` : ""}
        </div>`).join("") : 
      `<div class="text-muted ms-3">${fallbackStr}</div>`;

    const certsHtml = Array.isArray(profile.certifications ?? []) && (profile.certifications ?? []).length > 0 ? 
      `<ul class="list-unstyled ms-3">
         ${(profile.certifications ?? []).map(c => `<li><i class="bi bi-award-fill text-warning me-2"></i>${c.certificationName || ""}${c.platform ? ` - ${c.platform}` : ""}</li>`).join("")}
       </ul>` : 
      `<div class="text-muted ms-3">${fallbackStr}</div>`;

    const expHtml = Array.isArray(profile.experience ?? []) && (profile.experience ?? []).length > 0 ? 
      (profile.experience ?? []).map(e => `
        <div class="preview-timeline-item">
          <div class="timeline-dot"><i class="bi bi-briefcase-fill"></i></div>
          <div class="timeline-content">
            <div class="timeline-title fw-bold">${e.role || ""} ${e.company ? `at ${e.company}` : ""} ${e.duration ? `(${e.duration})` : ""}</div>
            ${e.responsibilities && e.responsibilities.length > 0 ? `
              <ul class="experience-responsibilities ms-3 mt-1 pl-3 small text-secondary">
                ${e.responsibilities.map(r => `<li>${r}</li>`).join("")}
              </ul>` : ""}
          </div>
        </div>`).join("") : 
      `<div class="text-muted ms-3">${fallbackStr}</div>`;

    const langList = profile.languages || profile.languagesKnown || [];
    const langHtml = langList.length > 0 ? 
      langList.map(l => `<span class="badge bg-secondary-subtle text-secondary me-1 mb-1">${l}</span>`).join("") : 
      `<span class="text-muted">${fallbackStr}</span>`;

    container.innerHTML = `
      <div class="preview-header mb-3 border-bottom pb-2">
        <div class="preview-name fw-extrabold fs-5"><i class="bi bi-person-fill text-primary me-2"></i>${pi.name || fallbackStr}</div>
        <div class="preview-contact row g-2 small text-secondary mt-1">
          <div class="col-sm-6"><i class="bi bi-envelope-fill me-2"></i>${pi.email || fallbackStr}</div>
          <div class="col-sm-6"><i class="bi bi-telephone-fill me-2"></i>${pi.phone || fallbackStr}</div>
          <div class="col-sm-6"><i class="bi bi-geo-alt-fill me-2"></i>${pi.location || fallbackStr}</div>
          <div class="col-sm-6"><i class="bi bi-linkedin me-2"></i>${pi.linkedin ? `<a href="${pi.linkedin.startsWith("http") ? pi.linkedin : "https://" + pi.linkedin}" target="_blank">${pi.linkedin}</a>` : fallbackStr}</div>
          <div class="col-sm-6"><i class="bi bi-github me-2"></i>${pi.github ? `<a href="${pi.github.startsWith("http") ? pi.github : "https://" + pi.github}" target="_blank">${pi.github}</a>` : fallbackStr}</div>
        </div>
      </div>
      
      <div class="preview-section">
        <div class="preview-section-title"><i class="bi bi-mortarboard-fill me-2"></i>Education</div>
        <div class="preview-timeline">${educationHtml}</div>
      </div>
      
      <div class="preview-section">
        <div class="preview-section-title"><i class="bi bi-tags-fill me-2"></i>Skills</div>
        <div class="preview-skills">${skillsHtml}</div>
      </div>
      
      <div class="preview-section">
        <div class="preview-section-title"><i class="bi bi-briefcase-fill me-2"></i>Experience</div>
        <div class="preview-timeline">${expHtml}</div>
      </div>
      
      <div class="preview-section">
        <div class="preview-section-title"><i class="bi bi-git me-2"></i>Projects</div>
        <div class="preview-projects">${projectsHtml}</div>
      </div>
      
      <div class="preview-section">
        <div class="preview-section-title"><i class="bi bi-award-fill me-2"></i>Certifications</div>
        <div class="preview-certs">${certsHtml}</div>
      </div>
      
      <div class="preview-section">
        <div class="preview-section-title"><i class="bi bi-translate me-2"></i>Languages</div>
        <div class="preview-languages">${langHtml}</div>
      </div>
    `;
  }

  /* ---------- Render Results ---------- */
  function renderResults(data) {
    lastAnalysisData = data;
    const resultsSection = $("#resultsSection");
    if (!resultsSection) return;

    const circumference = 2 * Math.PI * 80;
    const scoreFill = $("#scoreCircleFill") || $(".score-circle-fill");
    const scoreNumber = $("#scoreNumber");
    const scoreGrade = $("#scoreGrade");
    const grade = getScoreGrade(data.atsScore);

    if (scoreFill) {
      const offset = circumference - (data.atsScore / 100) * circumference;
      scoreFill.style.strokeDasharray = circumference;
      scoreFill.style.strokeDashoffset = circumference;
      const strokeColor = grade.class === "excellent" ? "#10b981"
        : grade.class === "good" ? "#6366f1"
        : grade.class === "fair" ? "#f59e0b" : "#ef4444";
      scoreFill.style.stroke = strokeColor;
      setTimeout(() => { scoreFill.style.strokeDashoffset = offset; }, 150);
    }

    if (scoreNumber) {
      scoreNumber.className = "score-number " + grade.class;
      animateCounter(scoreNumber, data.atsScore, 1500);
    }

    if (scoreGrade) {
      scoreGrade.textContent = grade.label;
      scoreGrade.className = "score-grade " + grade.class;
    }

    const completenessPct = data.atsBreakdown.completeness ? Math.round((data.atsBreakdown.completeness / 15) * 100) : 0;
    const keywordCoveragePct = data.atsBreakdown.keywordCoverage ? Math.round((data.atsBreakdown.keywordCoverage / 15) * 100) : 0;
    const experiencePct = data.atsBreakdown.experience ? Math.round((data.atsBreakdown.experience / 20) * 100) : 0;
    const educationPct = data.atsBreakdown.education ? Math.round((data.atsBreakdown.education / 20) * 100) : 0;
    const skillMatchPct = data.atsBreakdown.skillMatch ? Math.round((data.atsBreakdown.skillMatch / 40) * 100) : 0;
    const projectsCertsPct = data.atsBreakdown.projectsCertifications ? Math.round((data.atsBreakdown.projectsCertifications / 10) * 100) : 0;

    updateWidget("widgetAtsScore", data.atsScore);
    updateWidget("widgetResumeQuality", completenessPct);
    updateWidget("widgetKeywordDensity", keywordCoveragePct);
    updateWidget("widgetConfidence", 92);

    const matchedCount = $("#widgetMatchedCount");
    const missingCount = $("#widgetMissingCount");
    if (matchedCount) animateCounter(matchedCount, data.matchedSkills.length, 1000);
    if (missingCount) animateCounter(missingCount, data.missingSkills.length, 1000);

    const matchedBadge = $("#matchedBadge");
    const missingBadge = $("#missingBadge");
    if (matchedBadge) matchedBadge.textContent = data.matchedSkills.length + " Found";
    if (missingBadge) missingBadge.textContent = data.missingSkills.length + " Gaps";

    const set = (id, val) => { const el = $(id); if (el) el.textContent = val; };
    set("#analyticsFormat", completenessPct + "%");
    set("#analyticsKeywords", keywordCoveragePct + "%");
    set("#analyticsExperience", projectsCertsPct + "%");
    set("#analyticsImpact", skillMatchPct + "%");

    const matchedContainer = $("#matchedSkills");
    if (matchedContainer) {
      matchedContainer.innerHTML = (data.matchedSkills || []).map((skill) =>
        `<span class="skill-badge matched" role="listitem" data-bs-toggle="tooltip" title="Found in resume">
          <i class="bi bi-check-circle-fill" aria-hidden="true"></i>${skill}
        </span>`
      ).join("");
    }

    const missingContainer = $("#missingSkills");
    if (missingContainer) {
      missingContainer.innerHTML = (data.missingSkills || []).map((skill) => {
        const s = typeof skill === "string" ? { name: skill, critical: false } : skill;
        return `<span class="skill-badge missing${s.critical ? " critical" : ""}" role="listitem" data-bs-toggle="tooltip" title="${s.critical ? "Critical missing keyword" : "Not found in resume"}">
          <i class="bi bi-${s.critical ? "exclamation-circle-fill" : "x-circle-fill"}" aria-hidden="true"></i>${s.name}
        </span>`;
      }).join("");
    }

    // AI Summary display with clean sections & spacing (Issue 5 & Issue 8)
    const summaryEl = $("#resumeSummary");
    if (summaryEl && (data.summary || data.resumeSummary)) {
      const skillsList = getSkillsArray(data.skills);
      const educationStr = getEducationString(data.education);
      let html = `
        <div class="summary-section mb-3">
          <h5 class="small-title text-primary mb-1" style="font-size:0.875rem;font-weight:700;"><i class="bi bi-cpu-fill me-2"></i>Professional Summary</h5>
          <p class="summary-desc text-secondary" style="font-size:0.8125rem;line-height:1.6;margin-bottom:0.75rem;">${data.summary || data.resumeSummary}</p>
        </div>
        <div class="row g-2 mb-3">
          <div class="col-md-6">
            <div class="summary-section">
              <h5 class="small-title text-primary mb-1" style="font-size:0.8125rem;font-weight:700;"><i class="bi bi-mortarboard-fill me-2"></i>Education</h5>
              <p class="summary-val text-secondary small" style="margin-bottom:0.25rem;">${educationStr}</p>
            </div>
          </div>
          <div class="col-md-6">
            <div class="summary-section">
              <h5 class="small-title text-primary mb-1" style="font-size:0.8125rem;font-weight:700;"><i class="bi bi-bullseye me-2"></i>Career Objective</h5>
              <p class="summary-val text-secondary small" style="margin-bottom:0.25rem;">Software Developer / Engineer</p>
            </div>
          </div>
        </div>
        <div class="summary-section mb-3">
          <h5 class="small-title text-primary mb-1" style="font-size:0.8125rem;font-weight:700;"><i class="bi bi-tags-fill me-2"></i>Core Skills</h5>
          <div class="skills-mini mt-1">
            ${skillsList.length > 0 ? skillsList.slice(0, 8).map(s => `<span class="badge bg-primary-subtle text-primary me-1 mb-1" style="font-size:0.6875rem;font-weight:600;padding:0.25rem 0.5rem;border-radius:2rem;">${s}</span>`).join("") : "Not Available"}
          </div>
        </div>
        <div class="summary-section mb-3">
          <h5 class="small-title text-primary mb-1" style="font-size:0.8125rem;font-weight:700;"><i class="bi bi-git me-2"></i>Key Projects</h5>
          <ul class="summary-list text-secondary pl-3 small" style="padding-left:1.15rem;margin-bottom:0.5rem;">
            ${Array.isArray(data.projects ?? []) && (data.projects ?? []).length > 0 ? (data.projects ?? []).slice(0, 2).map(p => `<li>${p.projectName || ""}</li>`).join("") : "<li>Not Available</li>"}
          </ul>
        </div>
        <div class="summary-section">
          <h5 class="small-title text-primary mb-1" style="font-size:0.8125rem;font-weight:700;"><i class="bi bi-award-fill text-warning me-2"></i>Certifications</h5>
          <ul class="summary-list text-secondary pl-3 small" style="padding-left:1.15rem;margin-bottom:0.5rem;">
            ${Array.isArray(data.certifications ?? []) && (data.certifications ?? []).length > 0 ? (data.certifications ?? []).slice(0, 2).map(c => `<li>${c.certificationName || ""}${c.platform ? ` - ${c.platform}` : ""}</li>`).join("") : "<li>Not Available</li>"}
          </ul>
        </div>
      `;
      summaryEl.innerHTML = html;
    }

    renderResumePreview(data);

    const suggestionsContainer = $("#aiSuggestions");
    if (suggestionsContainer) {
      suggestionsContainer.innerHTML = (data.recommendations || []).map((s, i) => `
        <div class="col-md-6 fade-in-section stagger-${(i % 4) + 1}">
          <div class="suggestion-card">
            <div class="suggestion-icon"><i class="bi ${s.icon || 'bi-lightbulb'}" aria-hidden="true"></i></div>
            <div>
              <div class="suggestion-title">${s.title || ""}</div>
              <p class="suggestion-text">${s.text || ""}</p>
            </div>
          </div>
        </div>`).join("");
    }

    const strengthsContainer = $("#strengthsList");
    if (strengthsContainer) {
      strengthsContainer.innerHTML = (data.strengths || []).map((s) => `
        <div class="col-md-6">
          <div class="insight-card strength">
            <div class="insight-icon"><i class="bi bi-check-lg" aria-hidden="true"></i></div>
            <p class="insight-text">${s}</p>
          </div>
        </div>`).join("");
    }

    const weaknessesContainer = $("#weaknessesList");
    if (weaknessesContainer) {
      weaknessesContainer.innerHTML = (data.weaknesses || []).map((w) => `
        <div class="col-md-6">
          <div class="insight-card weakness">
            <div class="insight-icon"><i class="bi bi-exclamation-lg" aria-hidden="true"></i></div>
            <p class="insight-text">${w}</p>
          </div>
        </div>`).join("");
    }

    const keywordTable = $("#keywordTableBody");
    if (keywordTable && data.keywordDensity) {
      const keywordsArray = Object.entries(data.keywordDensity || {}).map(([kw, val]) => ({
        keyword: kw,
        count: val ? (val.count || 0) : 0,
        density: val ? (val.density || 0) : 0,
        status: val ? (val.status || (val.density >= 70 ? "Optimal" : val.density >= 40 ? "Moderate" : "Low")) : "Low"
      }));

      keywordTable.innerHTML = (keywordsArray || []).map((k) => `
        <tr>
          <td><strong>${k.keyword}</strong></td>
          <td>${k.count}</td>
          <td><span class="density-bar"><span class="density-bar-fill" style="width:${k.density}%"></span></span>${k.density}%</td>
          <td><span class="badge ${k.density >= 70 ? "bg-success" : k.density >= 40 ? "bg-warning text-dark" : "bg-danger"}">${k.status}</span></td>
        </tr>`).join("");
    }

    updateCharts(data);

    const exportBar = $("#exportBar");
    if (exportBar) exportBar.classList.add("visible");

    resultsSection.classList.add("visible");
    setTimeout(() => resultsSection.scrollIntoView({ behavior: "smooth", block: "start" }), 200);

    initScrollReveal();
    initTooltips();
  }

  function updateWidget(id, value) {
    const widget = document.getElementById(id);
    if (!widget) return;
    const valueEl = widget.querySelector(".widget-value");
    const progressBar = widget.querySelector(".widget-progress-bar");
    if (valueEl) {
      valueEl.textContent = "0%";
      setTimeout(() => animateCounter(valueEl, value, 1200, "%"), 200);
    }
    if (progressBar) {
      progressBar.style.width = "0%";
      setTimeout(() => { progressBar.style.width = value + "%"; }, 300);
    }
  }

  /* ---------- Export Features ---------- */
  function initExport() {
    const pdfBtn = $("#downloadPdfBtn");
    const summaryBtn = $("#downloadSummaryBtn");
    const printBtn = $("#printReportBtn");

    if (pdfBtn) {
      pdfBtn.addEventListener("click", () => {
        if (!lastAnalysisData) return;
        const element = $("#resultsSection");
        if (typeof html2pdf === "undefined") {
          showToast("PDF library loading. Please try again.", "warning");
          return;
        }
        showToast("Generating PDF report...", "info");
        html2pdf().set({
          margin: 10,
          filename: "resume-analysis-report.pdf",
          image: { type: "jpeg", quality: 0.98 },
          html2canvas: { scale: 2, useCORS: true },
          jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
        }).from(element).save().then(() => showToast("PDF downloaded successfully!", "success"));
      });
    }

    if (summaryBtn) {
      summaryBtn.addEventListener("click", () => {
        if (!lastAnalysisData) return;
        const d = lastAnalysisData;
        const text = [
          "AI RESUME ANALYZER — ANALYSIS SUMMARY",
          "========================================",
          "",
          `ATS Score: ${d.atsScore || 0}/100`,
          `Resume Quality: ${d.resumeQuality || 0}%`,
          `Skill Match: ${d.skillMatch || 0}%`,
          `Keyword Density: ${d.keywordDensity || 0}%`,
          `Analysis Confidence: ${d.analysisConfidence || 0}%`,
          "",
          "MATCHED SKILLS:",
          (d.matchedSkills || []).join(", "),
          "",
          "MISSING SKILLS:",
          (d.missingSkills || []).map((s) => (typeof s === "string" ? s : s ? (s.name || "") : "")).join(", "),
          "",
          "SUMMARY:",
          d.resumeSummary || d.summary || "",
          "",
          "STRENGTHS:",
          ...(d.strengths || []).map((s) => "• " + s),
          "",
          "WEAKNESSES:",
          ...(d.weaknesses || []).map((w) => "• " + w),
          "",
          "RECOMMENDATIONS:",
          ...(d.recommendations || d.suggestions || []).map((s) => "• " + (s.title || "") + ": " + (s.text || "")),
        ].join("\n");

        const blob = new Blob([text], { type: "text/plain" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "analysis-summary.txt";
        a.click();
        URL.revokeObjectURL(url);
        showToast("Summary downloaded!", "success");
      });
    }

    if (printBtn) {
      printBtn.addEventListener("click", () => window.print());
    }
  }

  /* ---------- Analyze Button ---------- */
  function initAnalyzeButton() {
    const analyzeBtn = $("#analyzeBtn");
    const fileInput = $("#resumeFile");
    const jobDescription = $("#jobDescription");
    if (!analyzeBtn) return;

    analyzeBtn.addEventListener("click", async () => {
      const hasFile = fileInput && fileInput.files.length > 0;
      const hasJobDesc = jobDescription && jobDescription.value.trim().length > 50;

      if (!hasFile) {
        showToast("Please upload your resume first.", "warning");
        $("#uploadZone")?.scrollIntoView({ behavior: "smooth", block: "center" });
        return;
      }
      if (!hasJobDesc) {
        showToast("Please enter a job description (at least 50 characters).", "warning");
        jobDescription?.focus();
        return;
      }

      analyzeBtn.classList.add("loading");
      analyzeBtn.disabled = true;

      // Start the backend fetch request immediately
      const formData = new FormData();
      formData.append("file", fileInput.files[0]);
      formData.append("jobDescription", jobDescription.value);

      const fetchPromise = fetch("/api/analyze", {
        method: "POST",
        body: formData
      });

      // Run progress bar animations
      await runAnalysisSteps();

      try {
        const response = await fetchPromise;
        if (!response.ok) {
          let errorDetail = "Server error";
          try {
            const errJson = await response.json();
            errorDetail = errJson.error || errJson.message || errorDetail;
          } catch (_) {
            errorDetail = await response.text() || errorDetail;
          }
          throw new Error(errorDetail);
        }
        const result = await response.json();
        renderResults(result);
        showToast("Analysis complete!", "success");
      } catch (err) {
        console.error(err);
        showToast("Analysis failed: " + err.message, "danger");
      } finally {
        analyzeBtn.classList.remove("loading");
        analyzeBtn.disabled = false;
      }
    });
  }

  /* ---------- Toast ---------- */
  function showToast(message, type = "info") {
    let toastContainer = $("#toastContainer");
    if (!toastContainer) {
      toastContainer = document.createElement("div");
      toastContainer.id = "toastContainer";
      toastContainer.className = "toast-container position-fixed bottom-0 end-0 p-3";
      toastContainer.style.zIndex = "9999";
      document.body.appendChild(toastContainer);
    }

    const bgClass = type === "success" ? "bg-success"
      : type === "warning" ? "bg-warning text-dark"
      : type === "danger" ? "bg-danger" : "bg-primary";

    const toastEl = document.createElement("div");
    toastEl.className = `toast align-items-center text-white ${bgClass} border-0`;
    toastEl.setAttribute("role", "alert");
    toastEl.innerHTML = `<div class="d-flex"><div class="toast-body">${message}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div>`;

    toastContainer.appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
    toast.show();
    toastEl.addEventListener("hidden.bs.toast", () => toastEl.remove());
  }

  /* ---------- Init ---------- */
  document.addEventListener("DOMContentLoaded", () => {
    initTheme();
    initNavbar();
    initScrollReveal();
    initLandingCounters();
    initTooltips();
    initFileUpload();
    initCharCounter();
    initAnalyzeButton();
    initExport();
  });
})();
