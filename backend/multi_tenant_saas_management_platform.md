# Multi-Tenant SaaS Management Platform

## 1. Platform Overview

The system is a **multi-tenant B2B SaaS platform** where one SaaS company manages multiple client organizations.

```text
YOUR SAAS COMPANY
│
├── SaaS Owner / Super Admin
│
├── SaaS Staff
│   ├── Subscription Staff
│   ├── Customer Support
│   ├── Finance Staff
│   └── Sales Staff
│
└── CLIENT ORGANIZATIONS
    ├── Organization A
    ├── Organization B
    └── Organization C
```

The SaaS company controls the overall platform, subscriptions, payments, customer support, sales, and client organizations.

Each client organization operates inside its own isolated tenant.

---

## 2. High-Level Architecture

```text
                         SaaS Platform
                              │
               ┌──────────────┴──────────────┐
               │                             │
        SaaS Administration            SaaS Operations
               │                             │
          Super Admin                    SaaS Staff
                                             │
                       ┌─────────────────────┼─────────────────────┐
                       │                     │                     │
                 Subscription             Support               Finance
                    Staff                  Staff                  Staff
                       │                     │                     │
                       └─────────────────────┼─────────────────────┘
                                             │
                                  CLIENT ORGANIZATIONS
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
              Organization A          Organization B          Organization C
                    │                        │                        │
                  Owner                    Owner                    Owner
                    │                        │                        │
              Menu / Contracts         Menu / Contracts         Menu / Contracts
              Payments                 Payments                 Payments
```

---

# 3. SaaS Owner / Super Admin

The Super Admin has complete control over the SaaS platform.

### Main Modules

- Dashboard
- Organizations
- SaaS Staff
- Subscriptions
- Payments
- Contracts
- Reports
- System Settings

### Dashboard

Possible metrics:

- Total Organizations
- Active Organizations
- Trial Organizations
- Expired Organizations
- Monthly Recurring Revenue
- Pending Payments
- Overdue Payments
- Active Subscriptions
- Support Tickets
- New Leads

---

# 4. SaaS Staff

SaaS staff work for the SaaS company.

## 4.1 Subscription Staff

Responsible for client subscriptions.

### Features

- Manage subscription plans
- Assign plans
- Upgrade / downgrade subscriptions
- Renew subscriptions
- Track expiration dates
- Trial management
- Subscription history
- Subscription invoices
- Subscription status

Example:

```text
Organization: ABC Restaurant
Plan: Premium
Status: Active
Start Date: 01-09-2026
Renewal Date: 01-10-2026
```

---

## 4.2 Customer Support

Responsible for helping client organizations.

### Features

- Support dashboard
- Tickets
- Client requests
- Complaints
- Ticket assignment
- Ticket priority
- Ticket status
- Internal notes
- Support history

Example statuses:

```text
OPEN
IN_PROGRESS
WAITING_FOR_CLIENT
RESOLVED
CLOSED
```

---

## 4.3 Finance Staff

Responsible for SaaS financial operations.

### Features

- Payments
- Invoices
- Due payments
- Refunds
- Payment history
- Revenue reports
- Organization billing
- Subscription billing

Example:

```text
Organization      Invoice       Amount       Status
-----------------------------------------------------
ABC Restaurant    INV-1001      $50          PAID
XYZ Restaurant    INV-1002      $50          PENDING
DEF Restaurant    INV-1003      $100         OVERDUE
```

---

## 4.4 Sales Staff

Responsible for acquiring new organizations.

### Features

- Leads
- Prospects
- Organizations
- Sales pipeline
- Follow-ups
- Sales activities
- Sales reports

Example sales pipeline:

```text
Lead
  ↓
Contacted
  ↓
Demo
  ↓
Proposal
  ↓
Negotiation
  ↓
Converted
```

---

# 5. Client Organizations

Each organization is a separate tenant.

```text
CLIENT ORGANIZATIONS
│
├── Organization A
│   ├── Owner
│   ├── Managers / Staff
│   ├── Menu
│   ├── Contracts
│   ├── Payments
│   ├── Subscription
│   ├── Reports
│   └── Settings
│
├── Organization B
│   └── Same structure
│
└── Organization C
    └── Same structure
```

The data of one organization must not be accessible to another organization.

---

# 6. Organization Owner

The organization owner manages their own business.

## Dashboard

Possible information:

- Sales
- Menu
- Active contracts
- Pending contracts
- Payments
- Subscription status
- Notifications
- Reports

## Menu

For organizations that need menu management:

```text
Menu
│
├── Categories
│   ├── Food
│   ├── Drinks
│   └── Dessert
│
└── MenuImage
└── Items
    ├── Item Name
    ├── Description
    ├── Price
    ├── Category
    ├── Image
    └── Availability
```

## Employees

```text
Employees
│
├── Employee List
├── Add Employee
├── Roles
├── Attendance
├── Leave
└── Employee Reports
```

## Contracts

```text
Contracts
│
├── Active Contracts
├── Pending Contracts
├── Expired Contracts
├── Contract Documents
└── Contract History
```

## Payments

```text
Payments
│
├── Payment History
├── Pending Payments
├── Invoices
├── Receipts
└── Payment Methods
```

## Subscription

```text
Subscription
│
├── Current Plan
├── Usage
├── Billing Cycle
├── Renewal Date
├── Invoice History
└── Upgrade / Downgrade
```

---

# 7. Recommended Role Hierarchy

```text
SaaS Owner / Super Admin
        │
        ├── SaaS Staff
        │     ├── Subscription Staff
        │     ├── Support Staff
        │     ├── Finance Staff
        │     └── Sales Staff
        │
        └── Client Organizations
              │
              ├── Organization Owner
              │
              └── Organization Staff
                    ├── Manager
                    ├── Employee
                    └── Other custom roles
```

---

# 8. Permission Model

A recommended permission structure is:

```text
ROLE
  ↓
PERMISSIONS
  ↓
MODULE
  ↓
ACTION
```

Actions can include:

- VIEW
- CREATE
- UPDATE
- DELETE
- APPROVE
- EXPORT
- MANAGE

Example:

```text
Finance Staff
│
├── Payment
│   ├── VIEW
│   ├── CREATE
│   └── EXPORT
│
└── Invoice
    ├── VIEW
    ├── CREATE
    └── UPDATE
```

An Organization Owner should only receive permissions for their own organization.

---

# 9. Multi-Tenant Data Isolation

Every organization-owned record should be associated with an organization.

Conceptually:

```text
Organization
    │
    ├── Users
    ├── Employees
    ├── Menu Items
    ├── Contracts
    ├── Payments
    └── Reports
```

Most organization-level tables should contain:

```text
organization_id
```

Example:

```text
menu_items

id
organization_id
category_id
name
description
price
status
created_at
updated_at
```

This allows the backend to enforce:

```text
User → Organization → Data
```

---

# 10. Core Business Entities

A possible high-level data model:

```text
SaaSCompany
│
├── Users
│
├── Roles
│
├── Permissions
│
├── SubscriptionPlans
│
├── Organizations
│
├── Invoices
├── Payments
├── SupportTickets
├── Leads
└── Reports

Organization
│
├── OrganizationUsers
├── Employees
├── Menus
├── MenuItems
├── Contracts
├── Payments
├── Subscriptions
└── Settings
```

---

# 11. Subscription Model

The SaaS company can offer multiple plans.

Example:

| Plan | Price | Users | Features |
|---|---:|---:|---|
| Basic | $20/month | 5 | Core features |
| Standard | $50/month | 20 | Advanced features |
| Premium | $100/month | Unlimited | All features |

Subscription states:

```text
TRIAL
ACTIVE
PAST_DUE
CANCELLED
EXPIRED
SUSPENDED
```

---

# 12. Payment Flow

```text
Organization
      ↓
Select Subscription
      ↓
Create Invoice
      ↓
Payment
      ↓
Payment Gateway
      ↓
Payment Confirmation
      ↓
Activate / Renew Subscription
      ↓
Receipt
```

The SaaS Finance Staff should be able to monitor the complete payment lifecycle.

---

# 13. Organization Onboarding

Recommended onboarding flow:

```text
Sales Lead
    ↓
Demo
    ↓
Organization Registration
    ↓
Create Organization
    ↓
Create Organization Owner
    ↓
Select Subscription
    ↓
Payment
    ↓
Activate Organization
    ↓
Owner Completes Setup
    ↓
Start Using SaaS
```

---

# 14. SaaS Admin Organization View

When a Super Admin opens an organization:

```text
ABC Restaurant
│
├── Overview
├── Owner
├── Users
├── Subscription
├── Payments
├── Invoices
├── Contracts
├── Support Tickets
├── Activity Logs
└── Settings
```

The SaaS company can therefore manage the customer relationship without directly mixing customer business data.

---

# 15. Recommended MVP

Do not build every feature initially.

## Version 1 — MVP

### SaaS Side

- Super Admin
- SaaS Staff
- Organization management
- User management
- Subscription plans
- Subscription management
- Invoice management
- Payment tracking
- Basic support tickets
- Basic dashboard
- Role and permission system

### Organization Side

- Organization Owner
- Organization Staff
- Dashboard
- Subscription
- Payments
- Invoices
- Contracts
- Basic organization settings
- Email notifications
- SMS notifications
---

# 18. Important Design Principle

The most important architectural rule is:

> **One SaaS platform → Many organizations → Each organization owns isolated data.**

The hierarchy should remain:

```text
SaaS Company
     │
     ├── SaaS Admin
     ├── SaaS Staff
     │
     └── Organizations
            │
            ├── Organization Owner
            ├── Organization Staff
            └── Organization Data
```

This structure gives you a scalable foundation for managing hundreds or thousands of client organizations from a single SaaS platform.
