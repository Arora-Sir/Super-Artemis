Pull request #10 has been received and reviewed.

The implementation cleanly reuses `com.limelight.ui.DragReorder` and adds the drag handle in `row_profile.xml`. The `ProfilesManager.move(from, to)` logic maintains insertion order in `LinkedHashMap`, and the added unit tests in `ProfilesManagerTest` verify persistence across reloads.

All checks passed. Merging pull request #10 will resolve this issue.
