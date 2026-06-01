# Small Builder Scope

EVE App Builder starts as a safe app planning and creation workflow.

## First Version

The first version should not generate or deploy large projects automatically.

It should help with:

- asking short app requirement questions
- writing a small project plan
- creating a safe file checklist
- preparing code after approval
- testing the result
- recording errors
- keeping rollback notes

## App Creation Flow

```text
idea -> questions -> plan -> approval -> create -> test -> fix -> save
```

## Minimum App Checklist

Every generated app should include:

- README
- clear entry file
- error handler
- simple health/test step
- no private keys in code
- rollback or backup note

## Rule

EVE asks first. EVE builds only after Aslam approves.
