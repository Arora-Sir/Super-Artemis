# Contributing Guidelines

Thank you for your interest in contributing to this project! We welcome contributions from the community and appreciate your efforts to make this project better.

## How to Contribute

### Reporting Issues

- Use the GitHub issue tracker to report bugs or request features
- Before creating a new issue, please search existing issues to avoid duplicates
- Provide clear, detailed descriptions with steps to reproduce bugs
- Include relevant system information and logs when applicable

### Code Contributions

#### Getting Started

1. Fork the repository
2. Create a new branch for your feature or bugfix: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Test your changes thoroughly
5. Commit your changes with clear, descriptive messages
6. Push your branch to your fork
7. Create a pull request

#### Code Standards

- Follow the existing code style and formatting conventions
- Write clear, readable code with appropriate comments
- Ensure all changes are sound
- Keep commits focused and atomic

#### Pull Request Guidelines

- Provide a clear description of what your PR does
- Reference any related issues using keywords like "Fixes #123"
- Include screenshots or examples if your changes affect the UI
- Be responsive to feedback and suggestions during code review

## Credit and attribution

Super Artemis is built from the work of many people. Every change that ports code, adapts a design, or answers a community request gives credit the same way.

### In commit messages

- When code is ported from another project, add a `Co-authored-by:` trailer with the author's GitHub noreply address (`<id>+<login>@users.noreply.github.com`, find the id with `gh api users/<login> --jq .id`).
- Reference every PR, commit or issue the change uses or answers on a `Refs:` line with the full `owner/repo#123` form, so GitHub links back to it from that thread.
- When the idea or design was adapted but no code was copied, say so in the body (for example: "Based on the design of qiin2333/moonlight-vplus#639 by @Yundi339.").
- Reference each source once in the commit that uses it. Do not repeat references in unrelated commits.

### In the credits file

- Add an entry to `app/src/main/res/raw/credits.json` when a feature ships, never before.
- Run `python scripts/render_credits.py --write` to refresh the README table. CI fails if the table is stale.
- Release notes take their Thanks section from `python scripts/render_credits.py --release-notes`.

### Taking changes from upstream and forks

Follow UPSTREAM.md: review the weekly "Upstream watch" issue and security advisories, keep original authorship with `git am` or `git cherry-pick` where the change applies cleanly, and add the source to `scripts/upstream_sources.json` when code comes from a new repository.

## Important Rules

### AI-Generated Code Policy

**AI-generated code is acceptable, but please make sure you have thoroughly reviewed and understand what it does.**

When using AI tools to generate code:
- Review every line of generated code carefully
- Understand the logic and potential implications
- Test the code thoroughly in your environment
- Ensure it follows project conventions and best practices
- Take responsibility for any issues that may arise from the generated code

***IMPORTANT: Don't trust AI generated tests. Test each modifications manually.***

### Code Review Process

- All contributions must go through code review
- Maintainers will review your pull request and provide feedback
- Address all feedback before the PR can be merged
- Be patient and respectful during the review process

## Development Setup

Please refer to SETUP.md for the development environment and README.md for an overview.

## Questions?

If you have questions about contributing, feel free to:
- Open an issue for discussion
- Contact the maintainers
- Check existing documentation

Thank you for contributing!

