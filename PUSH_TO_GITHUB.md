# Push v6 to GitHub

From PowerShell in this folder:

```powershell
git init
git branch -M main
git add .
git commit -m "fix: align Kotlin with current CloudStream"
git remote add origin https://github.com/huyndb/hh-cloudstream-repo.git
git push -u origin main --force
```

If `origin` already exists:

```powershell
git remote set-url origin https://github.com/huyndb/hh-cloudstream-repo.git
git push -u origin main --force
```

Open the NEW Actions run created by this commit. Do not re-run an older workflow run.
