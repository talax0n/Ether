package dev.ether.app.domain

/** Skills and roles people pick from. Anything else a person types is kept as they wrote it. */
object TagCatalog {
    val skills = listOf(
        "Android", "Arduino", "AWS", "C", "C++", "Canva", "Compose", "Copywriting", "Data analysis", "Django",
        "Docker", "Excel", "FastAPI", "Figma", "Finance", "Firebase", "Flutter", "Go", "Google Cloud", "Illustrator",
        "iOS", "Java", "JavaScript", "Kotlin", "LangChain", "Laravel", "LLMs", "Machine learning", "Marketing",
        "Motion", "Next.js", "Node.js", "Notion", "Pandas", "PHP", "Pitching", "PostgreSQL", "Prompting",
        "Prototyping", "Public speaking", "Python", "PyTorch", "Raspberry Pi", "React", "React Native", "Research",
        "Rust", "scikit-learn", "Solidity", "SQL", "Swift", "Tailwind", "TensorFlow", "TypeScript", "Unity",
        "User research", "UI/UX", "Video editing", "Vue", "3D printing", "IoT",
    )
    val roles = listOf(
        "Frontend", "Backend", "Fullstack", "Mobile", "Designer", "AI", "ML", "Data", "Hardware", "PM", "Business",
        "Marketing", "Pitcher", "DevOps", "Game dev",
    )
}

/** [selected] plus [raw], spelled the catalog's way when it matches one, and skipped when blank or already in. */
fun addTag(selected: List<String>, raw: String, catalog: List<String>): List<String> {
    val typed = raw.trim().trimEnd(',').trim()
    if (typed.isEmpty() || selected.any { it.equals(typed, ignoreCase = true) }) return selected
    return selected + (catalog.firstOrNull { it.equals(typed, ignoreCase = true) } ?: typed)
}

/** Catalog entries containing [query] that aren't picked yet, prefix matches first. */
fun tagSuggestions(query: String, catalog: List<String>, selected: List<String>, limit: Int = 6): List<String> {
    val q = query.trim()
    return catalog
        .filter { c -> selected.none { it.equals(c, ignoreCase = true) } && (q.isEmpty() || c.contains(q, ignoreCase = true)) }
        .sortedBy { if (it.startsWith(q, ignoreCase = true)) 0 else 1 }
        .take(limit)
}
