# Priority Logic

The core value of Prima-Focus is its deterministic and reproducible task prioritization formula.

## The Formula

`priorityScore = 10 * categoryWeight + 6 * hasDate + 8 * timeUrgency - 2 * ln(1 + subtasksCount) - 0.02 * estimatedMinutes - 0.5 * ageDays + manualBoost`

### Variables
- **categoryWeight**: Float mapped from the task's category and subcategory (see `categories.md`).
- **hasDate**: Boolean (1 if true, 0 if false).
- **timeUrgency**: 
  - `1.0` if the scheduled time is within the next 2 hours.
  - `0.6` if the scheduled time is within the next 24 hours.
  - `0.0` otherwise.
- **subtasksCount**: Integer representing the number of subtasks.
- **estimatedMinutes**: Integer representing the estimated effort in minutes.
- **ageDays**: Float representing the number of days since the task was created.
- **manualBoost**: Float, default `5`. Used for manual pinning or boosting.

### Rules
1. **High Priority Floor**: If `categoryWeight >= 4.0`, the `priorityScore` is clamped to a minimum of `70`.
2. **Automatic Projects**: If `estimatedMinutes > 180` OR `subtasksCount > 10`, the task is automatically marked as `isProject = true`.
