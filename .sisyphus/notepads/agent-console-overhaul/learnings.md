## Task 4 - CI/CD + Community Files (2026-03-13)

### Patterns / Conventions
- GitHub Actions: use `gradle/actions/setup-gradle@v3` (not deprecated `gradle/gradle-build-action`)
- `actions/upload-artifact@v4` for lint report upload (use v4, not v3 - v3 deprecated)
- `actions/setup-java@v4` with `distribution: 'temurin'` is the current standard
- Topics API: `PUT /repos/{owner}/{repo}/topics` with `names[]` array, returns sorted topics
- Community files in `.github/` directory raise GitHub health score significantly
- Issue templates use YAML front-matter (`name`, `about`, `title`, `labels`) for GitHub UI

### Successful Approaches
- `gh api --method PUT repos/owner/repo/topics --field 'names[]=topic'` works cleanly
- README CI badge format: `[![Name](badge_url)](workflow_url)`
- Combining badge + build-from-source section + clean title in one README edit is efficient
