# GitHub Pages Deployment Specification

This document details the GitHub Pages publishing configuration for **MIORA**.

## Current Deployment Configuration (Live)

GitHub Pages is officially enabled and configured on the repository:

- **Source**: Branch `main`, Directory `/docs`
- **Engine**: Native GitHub Pages static server with `.nojekyll` bypass
- **Public URL**: [`https://mefahim.github.io/MIORA/`](https://mefahim.github.io/MIORA/)
- **Trigger**: Every push to the `main` branch automatically deploys the landing page.

---

## Alternative: GitHub Actions Workflow (`.github/workflows/pages.yml`)

If you prefer to publish via GitHub Actions (using the official `actions/deploy-pages` action) instead of branch-based publishing, follow these steps:

1. In GitHub Repository Settings &rarr; **Pages** &rarr; **Build and deployment** &rarr; **Source**, select **GitHub Actions**.
2. Refresh your `gh` CLI OAuth token with workflow permissions:
   ```bash
   gh auth refresh -s workflow
   ```
3. Commit the workflow below to `.github/workflows/pages.yml`:

```yaml
name: Deploy GitHub Pages

on:
  push:
    branches:
      - main
    paths:
      - 'docs/**'
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: 'pages'
  cancel-in-progress: false

jobs:
  deploy:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Source
        uses: actions/checkout@v4

      - name: Setup Pages
        uses: actions/configure-pages@v4

      - name: Upload Pages Artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: './docs'

      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```
