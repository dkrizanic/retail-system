# Project Brief: Shopping Cart Microservice

## 1. Background

We are building a small retail system that lets customers buy products through digital channels. The system follows a microservice architecture.

This project covers one component: the **Shopping Cart** service.

## 2. Scope

- **In scope**: Backend only. That means the Shopping Cart service, its API, and how it stores data.
- **Out of scope**:
  - Frontend or UI
  - Product metadata, which the separate Product Catalog service handles

## 3. Functional Requirements

| ID | Requirement |
| :--- | :--- |
| **FR1** | **Get cart**: Return the current contents of a specific customer's cart. |
| **FR2** | **Add item**: Add a specific item to a customer's cart. |
| **FR3** | **Remove item**: Remove a specific item from a customer's cart. |
| **FR4** | **Evict cart**: Clear or delete a customer's entire cart. |
| **FR5** | **Sales statistics**: Return how many offers with a given offer ID and action were sold during a given time period. |

## 4. Data Model

### 4.1 Cart Item

Each item in a cart stores:

| Attribute | Description |
| :--- | :--- |
| **Offer ID** | ID of the offer the item represents, e.g. a specific Philips TV. Product details are stored in the Product Catalog. |
| **Action** | The operation for this item. Allowed values are listed in the table below. |
| **Prices** | A list of price objects. It must contain at least one price. |

Allowed actions:

| Action | Meaning | Example |
| :--- | :--- | :--- |
| **ADD** | Buying a new item | Buying a TV |
| **MODIFY** | Upgrading an existing item | Moving to a bigger subscription |
| **DELETE** | Removing an existing item | Cancelling a subscription |

### 4.2 Price

A price is one of two types:

| Type | Attributes | Example |
| :--- | :--- | :--- |
| **Recurring price** | Number of recurrences (including the time unit) + price value | 12 months, 24 months, 7 days |
| **One-time price** | Price value only | Single upfront payment |

## 5. Business Rules / Validation

- **BR1**: Each item must have at least one price.
- **BR2**: Every attribute of items and prices is mandatory and must hold a valid value. For example: no nulls, no empty strings, no negative or zero values where they make no sense, and only allowed values for enum fields like action.

## 6. Technical Constraints (Recommended)

- **Language/Framework**: Java with Spring Boot (`4.1.2`, Java `26` / `25 LTS`, Maven)
- **Database**: MongoDB (`8.0 LTS` / `9.0`)
- **Architecture**: A standalone microservice (Pragmatic DDD + Modular Layered Architecture)

## 7. Deliverables

1. A working backend that meets the requirements above.
2. The full source code.
3. A presentation or explanation of the solution, covering design decisions and how to run it.
