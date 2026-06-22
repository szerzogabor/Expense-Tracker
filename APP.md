# Expense Tracker Mobile App - Product Requirements Document (PRD)

## 1. Product Vision

A personal finance mobile application for individual users who want to understand where their money goes.

The application is designed for:

* Personal use
* Offline-first operation
* Local storage only
* Fast transaction entry
* Detailed tracking and analysis
* No budgeting/planning focus

Primary goal:

> Help users understand where their money goes through simple transaction tracking and powerful filtering, categorization, tagging, and account management.

---

# 2. Core Principles

## Offline First

* No user accounts
* No cloud synchronization
* All data stored locally on device

## Fast Data Entry

Adding a transaction should require:

Required fields:

* Amount
* Description

Transaction type selected via:

* Expense
* Income

Default date:

* Today

User can override date.

## Flexible Analysis

Users should be able to analyze:

* Expenses
* Income
* Accounts
* Categories
* Tags
* Transfers
* Balance adjustments

---

# 3. Transaction Types

## Expense

Money leaving an account.

Examples:

* Grocery shopping
* Fuel
* Rent

## Income

Money entering an account.

Examples:

* Salary
* Bonus

## Transfer

Transfer between two user-owned accounts.

Characteristics:

* Not counted as income
* Not counted as expense
* Affects balances only

Fields:

* Source account
* Destination account
* Amount

## Balance Adjustment

Used when real account balance differs from calculated balance.

Example:

Calculated:

350,000 HUF

Actual:

352,500 HUF

Adjustment:

+2,500 HUF

Characteristics:

* Appears in transaction history
* Affects account balance

---

# 4. Transaction Fields

Common fields:

* Transaction type
* Amount
* Description
* Date
* Account (mandatory)
* Category
* Tags
* Note

Optional:

* Category
* Tags
* Note

Mandatory:

* Amount
* Description
* Account

---

# 5. Categories

## General

* Single category per transaction
* Category optional

If not selected:

* Automatically assigned to "Other"

## User Categories

Users can:

* Create
* Edit
* Reorder

Users cannot create category hierarchies.

## Category Deletion

When category deleted:

All affected transactions become:

* Other

## Category Search

Supported in category picker.

## Category Learning Rules

Application stores mappings such as:

* Lidl → Grocery
* MOL → Transport

Features:

* Visible to users
* Editable by users

When creating transaction:

Application suggests category.

Application must NOT auto-assign category.

User decides.

---

# 6. Tags

## General

Tags are:

* Free-text
* Auto-created

Examples:

* vacation
* family
* dog

## Multiple Tags

A transaction may contain multiple tags.

## Tag Display

Tags appear:

* Transaction list
* Transaction details

## Tag Search

Supported.

## Tag Filtering

Supported.

Modes:

* AND
* OR

User selectable.

## Tag Summary

Supported.

Metrics:

* Transaction count
* Total amount

If one transaction has multiple tags:

Full amount contributes to every tag.

Example:

100,000 HUF

Tags:

* vacation
* family

Results:

vacation = 100,000

family = 100,000

## Unused Tags

Unused tags remain stored.

They are never automatically deleted.

## Tags Screen

Dedicated tags screen.

Features:

* Search
* Sorting

Sort options:

* Name
* Transaction count
* Amount

User selectable.

Selecting a tag opens filtered transaction list.

---

# 7. Accounts

## General

User-defined accounts.

Examples:

* OTP
* Revolut
* Cash

## Opening Balance

Optional.

## Account Required

Every transaction must belong to an account.

## Account Features

Users can:

* Create
* Edit
* Reorder
* Archive

## Account Appearance

Each account may have:

* Icon
* Color

## Default Account

Supported.

When archived:

User must select new default account.

## Archive Rules

Accounts can be archived with non-zero balance.

Application shows warning before archiving.

## Account Detail Screen

Displays:

Current totals:

* Current balance
* Income total
* Expense total
* Transfer totals

Time-period totals:

* Current balance
* Income total
* Expense total
* Transfer totals

Both views supported.

## Account Filtering

Supported.

User may select:

* One account
* Multiple accounts

---

# 8. Balances

## Current Balance

Defined as:

Sum of all active account balances.

Displayed on dashboard.

## Historical Recalculation

Supported.

If user inserts past transaction:

Entire balance history recalculated.

## Negative Balance

If transaction creates negative balance:

Application warns user.

Transaction may still be saved.

---

# 9. Dashboard

Displays selected period.

Application remembers last selected period.

Dashboard contains:

## Financial Summary

* Income
* Expenses
* Current Total Balance

## Accounts Section

Current balances for all accounts.

## Category Summary

For all categories:

Displays:

* Total amount
* Transaction count

Sorting:

* Amount
* Name
* Transaction count

User selectable.

Selecting category opens filtered transactions.

## Recent Transactions

Shows latest transactions.

---

# 10. Transaction List

## Sorting

Default:

Newest first.

## Grouping

Grouped by date.

Example:

2026-06-22

* Transaction A
* Transaction B

## Search

Search only:

* Description

Not:

* Tags
* Categories
* Notes

## Quick Actions

Swipe actions supported:

* Edit
* Delete

Delete requires confirmation.

## Account Visibility

Account displayed in transaction list.

## Transfer Visibility

Transfers displayed in transaction list.

## Balance Adjustments

Displayed in transaction list.

---

# 11. Transaction Detail Screen

Displays full transaction information.

User can:

* Edit
* Delete
* Duplicate

Clickable:

* Category
* Tags

Selecting them opens filtered transaction list.

---

# 12. Transaction Creation

Transaction types:

* Expense
* Income

Selected via two large buttons.

After save:

User may choose:

* Save
* Save & New

Unsaved changes warning required.

---

# 13. Duplicate & Reuse

## Duplicate

Supported.

Creates editable copy.

## Reuse

Supported.

Creates new transaction from previous one.

Date defaults to current date.

---

# 14. Date Handling

Default date:

* Today

Future dates allowed.

Future transactions handling:

User can choose whether reports include them.

---

# 15. Reporting

Supported dimensions:

* Categories
* Tags
* Accounts

Reports include:

* Income
* Expenses

---

# 16. CSV Export

Single export operation.

Contains:

* Transactions
* Accounts
* Categories
* Tags
* Learning rules

Exported into one CSV structure.

---

# 17. CSV Import

Supported.

Modes:

* Replace existing data
* Merge with existing data

User chooses before import.

---

# 18. Security

Version 1:

* No PIN
* No biometrics

---

# 19. Recurring Transactions

## Recurrence Engine

Supports:

* Daily
* Weekly
* Monthly
* Yearly
* Full RFC 5545 RRULE support

## Storage Model

Recurring rule stored separately.

Transactions generated only when occurrence becomes due.

## Generation Modes

User configurable per recurring rule.

Default:

Automatic creation.

Alternative:

Pending approval.

## Pending Transactions

User may choose whether reports include them.

## Recurring Edit Options

User may choose:

* This occurrence only
* This and future occurrences
* All occurrences
