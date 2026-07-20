# XnetMLops Showcase Data

`showcase_data.sql` creates a deterministic, linked dataset for the public
XnetMLops 1.0.0 showcase. It covers DPP, MTP, MEP, SMP, and XAA without storing
working infrastructure credentials or calling external services.

Run it against a disposable showcase database only:

```bash
mysql --default-character-set=utf8mb4 XnetMLops < demo/showcase_data.sql
```

The seed is safe to re-run. Use `cleanup_showcase_data.sql` to remove only the
records owned by this showcase dataset. Shared reference rows, the two legacy
`KB-DEMO-001/002` samples, the default tenant, and platform version catalogs are
intentionally retained. Back up the target database before applying or removing
showcase data.
