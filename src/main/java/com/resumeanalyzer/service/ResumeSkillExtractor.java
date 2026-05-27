package com.resumeanalyzer.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.resumeanalyzer.dto.SkillsDetail;

public class ResumeSkillExtractor {

    private static final Map<String, List<Pattern>> SKILL_PATTERNS = new LinkedHashMap<>();
    private static final Map<String, String> SYNONYM_MAP = new LinkedHashMap<>();
    private static final Map<String, String> CANONICAL_CASE_MAP = new LinkedHashMap<>();
    private static final Set<String> KNOWN_SKILLS;
    private static final Set<String> COMMON_WORDS = new HashSet<>(Arrays.asList(
        "and", "or", "with", "for", "the", "a", "an", "in", "on", "by", "to", "of", "from",
        "experience", "skills", "development", "developer", "candidate", "project", "projects", "team", "using",
        "technology", "technologies", "engineering", "work", "resume", "summary", "certified", "certificate",
        "internship", "intern", "training", "management", "business", "system", "systems"
    ));

    static {
        addSkillPatterns("Java", "java");
        addSkillPatterns("Spring Boot", "spring boot", "springboot", "spring mvc", "spring framework", "spring");
        addSkillPatterns("Hibernate", "hibernate");
        addSkillPatterns("Maven", "maven");
        addSkillPatterns("Gradle", "gradle");
        addSkillPatterns("React", "react", "reactjs", "react.js");
        addSkillPatterns("Angular", "angular", "angularjs");
        addSkillPatterns("Node.js", "node.js", "nodejs", "node");
        addSkillPatterns("Express", "express", "express.js");
        addSkillPatterns("REST API", "rest api", "rest apis", "restful api", "restful apis", "restful", "api development", "backend services", "api endpoints", "rest services");
        addSkillPatterns("Microservices", "microservice", "microservices");
        addSkillPatterns("GraphQL", "graphql");
        addSkillPatterns("MongoDB", "mongodb", "mongo");
        addSkillPatterns("MySQL", "mysql", "my sql");
        addSkillPatterns("PostgreSQL", "postgresql", "postgres");
        addSkillPatterns("Oracle", "oracle");
        addSkillPatterns("Git", "git", "github", "gitlab", "bitbucket");
        addSkillPatterns("Jenkins", "jenkins");
        addSkillPatterns("Docker", "docker", "docker containers");
        addSkillPatterns("Kubernetes", "kubernetes", "k8s");
        addSkillPatterns("AWS", "aws", "amazon web services", "aws cloud");
        addSkillPatterns("Azure", "azure", "microsoft azure");
        addSkillPatterns("GCP", "gcp", "google cloud", "google cloud platform");
        addSkillPatterns("CI/CD", "ci/cd", "cicd", "continuous integration", "continuous delivery", "continuous deployment");
        addSkillPatterns("Python", "python", "py");
        addSkillPatterns("TensorFlow", "tensorflow", "tf");
        addSkillPatterns("PyTorch", "pytorch");
        addSkillPatterns("Machine Learning", "machine learning", "ml");
        addSkillPatterns("Deep Learning", "deep learning", "dl");
        addSkillPatterns("NLP", "nlp", "natural language processing");
        addSkillPatterns("Computer Vision", "computer vision", "cv");
        addSkillPatterns("Data Science", "data science");
        addSkillPatterns("LangChain", "langchain");
        addSkillPatterns("Large Language Model", "llm", "large language model", "large language models");
        addSkillPatterns("Vector Database", "vector database", "vector databases", "chromadb", "pinecone", "milvus", "weaviate");
        addSkillPatterns("MLOps", "mlops");
        addSkillPatterns("RAG", "rag", "retrieval augmented generation");
        addSkillPatterns("Prompt Engineering", "prompt engineering");
        addSkillPatterns("OpenAI", "openai");
        addSkillPatterns("Gemini", "gemini");
        addSkillPatterns("Claude", "claude");
        addSkillPatterns("Power BI", "power bi", "powerbi");
        addSkillPatterns("Tableau", "tableau");
        addSkillPatterns("Excel", "excel");
        addSkillPatterns("Postman", "postman");
        addSkillPatterns("Swagger", "swagger");
        addSkillPatterns("Pandas", "pandas");
        addSkillPatterns("NumPy", "numpy");
        addSkillPatterns("Keras", "keras");
        addSkillPatterns("Scikit-learn", "scikit-learn", "sklearn");
        addSkillPatterns("OpenCV", "opencv");
        addSkillPatterns("NLTK", "nltk");
        addSkillPatterns("Matplotlib", "matplotlib");
        addSkillPatterns("Seaborn", "seaborn");
        addSkillPatterns("Vue", "vue", "vue.js");
        addSkillPatterns("React Native", "react native");
        addSkillPatterns("Kotlin", "kotlin");
        addSkillPatterns("C#", "c#", "c sharp", "csharp");
        addSkillPatterns("C++", "c++", "cpp");
        addSkillPatterns("HTML", "html", "html5");
        addSkillPatterns("CSS", "css", "css3");
        addSkillPatterns("SQL", "sql");
        addSkillPatterns("JavaScript", "javascript", "js");
        addSkillPatterns("TypeScript", "typescript", "ts");
        addSkillPatterns("Flutter", "flutter");
        addSkillPatterns("Android", "android");
        addSkillPatterns("Ruby", "ruby");
        addSkillPatterns("PHP", "php");
        addSkillPatterns("Swift", "swift");
        addSkillPatterns("R", " r ");

        for (String canonical : SKILL_PATTERNS.keySet()) {
            CANONICAL_CASE_MAP.put(canonical.toLowerCase(Locale.ENGLISH), canonical);
            SYNONYM_MAP.put(canonical.toLowerCase(Locale.ENGLISH), canonical);
        }

        SYNONYM_MAP.put("github", "Git");
        SYNONYM_MAP.put("gitlab", "Git");
        SYNONYM_MAP.put("github.com", "Git");
        SYNONYM_MAP.put("spring", "Spring Boot");
        SYNONYM_MAP.put("spring mvc", "Spring Boot");
        SYNONYM_MAP.put("spring framework", "Spring Boot");
        SYNONYM_MAP.put("restful api", "REST API");
        SYNONYM_MAP.put("api development", "REST API");
        SYNONYM_MAP.put("backend services", "REST API");
        SYNONYM_MAP.put("aws cloud", "AWS");
        SYNONYM_MAP.put("amazon web services", "AWS");
        SYNONYM_MAP.put("reactjs", "React");
        SYNONYM_MAP.put("react.js", "React");
        SYNONYM_MAP.put("js", "JavaScript");
        SYNONYM_MAP.put("typescript", "TypeScript");
        SYNONYM_MAP.put("postgres", "PostgreSQL");
        SYNONYM_MAP.put("postresql", "PostgreSQL");
        SYNONYM_MAP.put("nodejs", "Node.js");
        SYNONYM_MAP.put("node.js", "Node.js");
        SYNONYM_MAP.put("ml", "Machine Learning");
        SYNONYM_MAP.put("dl", "Deep Learning");
        SYNONYM_MAP.put("llm", "Large Language Model");
        SYNONYM_MAP.put("gcp", "GCP");
        SYNONYM_MAP.put("ci/cd", "CI/CD");
        SYNONYM_MAP.put("cicd", "CI/CD");
        SYNONYM_MAP.put("data science", "Data Science");
        SYNONYM_MAP.put("computer vision", "Computer Vision");
        SYNONYM_MAP.put("natural language processing", "NLP");
        SYNONYM_MAP.put("postgresql", "PostgreSQL");
        SYNONYM_MAP.put("aws", "AWS");
        SYNONYM_MAP.put("azure", "Azure");
        SYNONYM_MAP.put("gcp", "GCP");

        KNOWN_SKILLS = Collections.unmodifiableSet(new HashSet<>(CANONICAL_CASE_MAP.keySet()));
    }

    private static void addSkillPatterns(String canonical, String... triggers) {
        List<Pattern> patterns = Arrays.stream(triggers)
            .map(String::trim)
            .map(t -> Pattern.compile("\\b" + Pattern.quote(t) + "\\b", Pattern.CASE_INSENSITIVE))
            .collect(Collectors.toList());
        SKILL_PATTERNS.put(canonical, patterns);
    }

    public static Set<String> extractSkillsFromText(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String lowerText = text.toLowerCase(Locale.ENGLISH);
        Set<String> found = new LinkedHashSet<>();

        for (Map.Entry<String, List<Pattern>> entry : SKILL_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(lowerText).find()) {
                    found.add(entry.getKey());
                    break;
                }
            }
        }

        for (String candidate : extractSectionCandidates(text)) {
            String normalized = normalizeSkill(candidate);
            if (normalized != null) {
                found.add(normalized);
            }
        }

        return found.stream()
            .map(ResumeSkillExtractor::normalizeSkill)
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> extractSectionCandidates(String text) {
        Set<String> candidates = new LinkedHashSet<>();
        String[] lines = text.split("\\r?\\n");
        String currentSection = null;

        Pattern headerPattern = Pattern.compile("^(skills|technical skills|projects|academic projects|certifications|licenses & certifications|experience|work experience|internships|training|summary)[:\s]*$", Pattern.CASE_INSENSITIVE);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                currentSection = null;
                continue;
            }
            Matcher headerMatcher = headerPattern.matcher(line);
            if (headerMatcher.find()) {
                currentSection = headerMatcher.group(1).toLowerCase(Locale.ENGLISH);
                continue;
            }
            if (currentSection != null || line.contains(",") || line.contains("|") || line.contains("/") || line.contains(";") || line.matches(".*\\b(certified|certificate|developed|project|experience|skills|skill|internship|training)\\b.*")) {
                candidates.addAll(splitCandidates(line));
            }
        }

        // also collect phrase-level candidates from the whole text for potential skills lists
        candidates.addAll(splitCandidates(text));
        return candidates;
    }

    private static Set<String> splitCandidates(String text) {
        Set<String> candidates = new LinkedHashSet<>();
        String[] parts = text.split("[,;|/\\n\\r]+");
        for (String part : parts) {
            String normalized = part.trim();
            if (normalized.isBlank()) continue;
            if (normalized.length() > 1 && !COMMON_WORDS.contains(normalized.toLowerCase(Locale.ENGLISH))) {
                candidates.add(normalized);
            }
        }
        return candidates;
    }

    public static String normalizeSkill(String skill) {
        if (skill == null) return null;
        String lower = skill.trim().toLowerCase(Locale.ENGLISH);
        if (lower.isEmpty()) return null;
        if (SYNONYM_MAP.containsKey(lower)) {
            return SYNONYM_MAP.get(lower);
        }
        if (KNOWN_SKILLS.contains(lower)) {
            return CANONICAL_CASE_MAP.get(lower);
        }
        return null;
    }

    public static SkillsDetail classifySkills(Set<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return SkillsDetail.createEmpty();
        }

        Set<String> languages = new LinkedHashSet<>();
        Set<String> frameworks = new LinkedHashSet<>();
        Set<String> libraries = new LinkedHashSet<>();
        Set<String> databases = new LinkedHashSet<>();
        Set<String> cloud = new LinkedHashSet<>();
        Set<String> devops = new LinkedHashSet<>();
        Set<String> tools = new LinkedHashSet<>();

        Set<String> programmingSkills = Set.of(
            "Java", "Python", "JavaScript", "TypeScript", "C#", "C++", "Kotlin", "SQL", "Ruby", "PHP", "Swift", "Dart", "R", "Go", "GoLang"
        );
        Set<String> frameworkSkills = Set.of(
            "Spring Boot", "React", "Angular", "Node.js", "Express", "Hibernate", "Flask", "Django", "FastAPI", "React Native", "Vue", "Laravel"
        );
        Set<String> librarySkills = Set.of(
            "TensorFlow", "PyTorch", "Pandas", "NumPy", "Keras", "Scikit-learn", "OpenCV", "NLTK", "Matplotlib", "Seaborn", "LangChain", "OpenAI", "GraphQL", "Prompt Engineering", "RAG"
        );
        Set<String> databaseSkills = Set.of(
            "MySQL", "PostgreSQL", "MongoDB", "Oracle", "Redis", "SQLite", "DynamoDB", "Elasticsearch", "Vector Database"
        );
        Set<String> cloudSkills = Set.of("AWS", "Azure", "GCP");
        Set<String> devopsSkills = Set.of("Docker", "Kubernetes", "CI/CD", "Jenkins", "Terraform", "Ansible", "Git", "GitHub", "GitLab");
        Set<String> toolSkills = Set.of("Maven", "Gradle", "Postman", "Swagger", "Power BI", "Tableau", "Excel", "Docker", "Kubernetes", "Git", "GitHub", "GitLab");

        for (String skill : skills) {
            if (programmingSkills.contains(skill)) {
                languages.add(skill);
            } else if (frameworkSkills.contains(skill)) {
                frameworks.add(skill);
            } else if (databaseSkills.contains(skill)) {
                databases.add(skill);
            } else if (cloudSkills.contains(skill)) {
                cloud.add(skill);
            } else if (devopsSkills.contains(skill)) {
                devops.add(skill);
            } else if (toolSkills.contains(skill)) {
                tools.add(skill);
            } else if (librarySkills.contains(skill)) {
                libraries.add(skill);
            } else {
                tools.add(skill);
            }
        }

        return new SkillsDetail(
            new ArrayList<>(languages),
            new ArrayList<>(frameworks),
            new ArrayList<>(libraries),
            new ArrayList<>(databases),
            new ArrayList<>(cloud),
            new ArrayList<>(devops),
            new ArrayList<>(tools)
        );
    }
}
