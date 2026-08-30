# Deployment backup retention

Deployment backups are named `frog2-deploy-YYYYMMDD_HHMMSS`. The retention tool
recognizes only that exact pattern and keeps the 10 newest backups by default.
Unknown directories are never selected.

Review without deleting anything:

```sh
src/tools/prune-deploy-backups.sh --target development --keep 10
src/tools/prune-deploy-backups.sh --target production --keep 10
```

After confirming the printed paths, deletion requires a separate explicit gate:

```sh
FROG2_BACKUP_PRUNE_APPROVED=yes \
  src/tools/prune-deploy-backups.sh --target development --keep 10 --apply
```

Do not prune the only rollback copy during a deployment window. Production
retention must be reviewed independently and is not installed as an unattended
timer by this repository.
