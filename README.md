# Maan-Meal 🌾 — Farm-to-Table E-Commerce Platform

A full-stack platform directly connecting **farmers** with **consumers**, eliminating middlemen and ensuring fair pricing, transparency, and fast delivery.

---

## 🚀 Quick Start

### 1. Set up the Backend

```powershell
cd java-backend

# Build and run with Maven
mvn spring-boot:run
```

The backend runs at: **http://localhost:5000**

### 2. Open the Frontend

Directly open `frontend/index.html` in your browser or run a simple local web server from the `frontend/` directory.

---

## 🔑 Demo Credentials

| Role     | Email                   | Password     |
|----------|-------------------------|--------------|
| Admin    | admin@maanmeal.com      | admin123     |
| Farmer   | rajesh@farm.com         | farmer123    |
| Consumer | anita@gmail.com         | consumer123  |

---

## 📁 Project Structure

```
Maan-Meal/
├── java-backend/           # Java Spring Boot backend
└── frontend/
    ├── index.html          # Landing page
    ├── auth/               # Login & Signup
    ├── consumer/           # Consumer pages
    ├── farmer/             # Farmer pages
    ├── admin/              # Admin panel
    ├── css/style.css       # Design system
    └── js/app.js           # Shared JS utilities
```

---

## 🎨 Features

- ✅ JWT-based authentication
- ✅ Role-based access (Farmer, Consumer, Admin)
- ✅ Admin approval for farmers
- ✅ Product management with image upload
- ✅ Shopping cart & secure checkout
- ✅ UPI / Card / COD payment simulation with QR
- ✅ Real-time order status tracking
- ✅ In-app notifications
- ✅ Ratings & reviews
- ✅ Dark agri-themed UI with glassmorphism
- ✅ Mobile-responsive design
