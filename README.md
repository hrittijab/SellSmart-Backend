SellSmart Backend
SellSmart is a backend RESTful API service built with Spring Boot that helps users manage their inventory, sales, and damaged goods with secure authentication and authorization.

Features
User Authentication: Register and login with JWT-based token authentication.

Inventory Management: Add, update, delete, and list inventory items.

Sales Tracking: Record daily sales, update or delete sales entries, and generate profit summaries.

Damaged Goods Management: Report, update, delete damaged goods, and track between dates.

Security: Email whitelist for registration, password hashing with BCrypt, and JWT token verification.

Cloud Database: Data persisted in Firebase Firestore.

Technologies
Java 17

Spring Boot 3.5.0

Firebase Firestore (NoSQL database)

JWT (JSON Web Tokens) for secure API access

BCrypt for password hashing

Maven for build and dependency management

