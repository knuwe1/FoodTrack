# 🍎 FoodTrack - Multi-Tenant Smart Food Inventory Management

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](android-client/)
[![PHP](https://img.shields.io/badge/PHP-777BB4?style=for-the-badge&logo=php&logoColor=white)](php-backend/)
[![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)](backend/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v2.1.0-brightgreen?style=for-the-badge)](https://github.com/knuwe1/FoodTrack/releases/tag/v2.1.0)

A comprehensive **Multi-Tenant** food inventory management system with intelligent barcode scanning, expiration tracking, and smart consumption management. Perfect for households, families, and shared living spaces. Never waste food again!

## ✨ Features

### 🏠 **Multi-Tenant Architecture**
- **👥 Household Management** - Separate inventories for different households
- **🔐 User Management** - Admin-controlled access per household
- **📍 Storage Locations** - Organize items by location (Fridge, Pantry, etc.)
- **📦 Package Management** - Track different package sizes and types
- **🔒 Data Isolation** - Complete separation between households

### 📱 **Android App**
- **🏷️ EAN Barcode Scanning** with OpenFoodFacts integration
- **📅 Smart Expiration Tracking** with FIFO consumption
- **⚠️ Low Stock Warnings** with configurable minimum quantities
- **📊 Batch Management** for different expiration dates separately
- **🗂️ Category Management** with automatic categorization
- **📈 Transaction History** with purchase/consumption tracking
- **🎯 Modern Material Design 3** with intuitive navigation
- **📍 Storage Location Selection** - Choose where items are stored
- **📦 Package Size Tracking** - Manage different package types

### 🌐 **Backend Options**
- **🐍 Python/FastAPI Backend** - Full-featured development server
- **🌍 PHP Backend** - Production-ready for shared hosting (1blu, etc.)
- **🔐 Secure Configuration** - Environment-based credentials
- **📊 RESTful API** - Complete CRUD operations with Multi-Tenant support
- **🔄 Real-time Sync** - Instant data synchronization
- **🛡️ Standalone Endpoints** - No external dependencies

### 🧠 **Smart Features**
- **🤖 Auto-fill Product Data** - Name, brand, quantity, unit, category from barcode
- **📦 Advanced Batch System** - Track items with different expiration dates separately
- **🔄 FIFO Consumption** - Automatically use oldest items first
- **⚡ Instant Warnings** - Visual alerts for expired/low-stock items
- **📱 Offline-Ready** - Local data caching for uninterrupted use
- **🏠 Household Switching** - Easy switching between different households

## 🚀 Quick Start

### 📱 **Android App Setup**
```bash
# Clone repository
git clone https://github.com/yourusername/FoodTrack.git
cd FoodTrack/android-client

# Open in Android Studio and build
# Or use command line:
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 🌍 **PHP Backend (Production)**
```bash
# 1. Upload php-backend/ files to your hosting
# 2. Create MySQL database
# 3. Run database/setup.sql
# 4. Configure credentials:

# Option A: .env file
cp .env.example .env
# Edit .env with your database credentials

# Option B: Web setup (recommended)
# Visit: https://yourdomain.com/setup.php
# Follow the setup wizard
# Delete setup.php after completion
```

### 🐍 **Python Backend (Development)**
```bash
cd backend/
pip install -r requirements.txt
uvicorn src.main:app --reload --host 0.0.0.0 --port 8000
```

## 📁 Project Structure

```
FoodTrack/
├── 📱 android-client/          # Android app (Kotlin)
│   ├── app/src/main/java/      # Source code
│   ├── app/src/main/res/       # Resources & layouts
│   └── build.gradle            # Dependencies
├── 🌍 php-backend/             # PHP backend (Production)
│   ├── endpoints/              # API endpoints
│   ├── database/               # SQL setup scripts
│   ├── .env.example           # Configuration template
│   └── README.md              # PHP setup guide
├── 🐍 backend/                 # Python backend (Development)
│   ├── src/                   # FastAPI source
│   ├── requirements.txt       # Python dependencies
│   └── README.md              # Python setup guide
└── 📚 docs/                   # Documentation
```

## 🔧 Configuration

### 🔐 **Secure Credentials**

**PHP Backend (.env):**
```env
DB_HOST=localhost
DB_NAME=your_database_name
DB_USER=your_username
DB_PASS="your_secure_password"
JWT_SECRET=your-random-secret-key
APP_DEBUG=false
```

**Android App:**
```kotlin
// Update Constants.kt
const val BASE_URL = "https://yourdomain.com/"
```

### 🔑 **Default Login**
- **Email:** `admin@foodtrack.com`
- **Password:** `admin`

## 📊 API Documentation

### 🔐 **Authentication**
```http
POST /api/v1/users/login-json
Content-Type: application/json

{
  "username": "admin@foodtrack.com",
  "password": "admin"
}
```

### 🏠 **Multi-Tenant Endpoints**
```http
# Get households
GET /households/

# Get storage locations for household
GET /api/v1/storage-locations/

# Get packages for household
GET /api/v1/packages/
```

### 🍎 **Food Items (Multi-Tenant)**
```http
# Get all items for current household
GET /api/v1/lebensmittel/

# Create item with storage location and package
POST /api/v1/lebensmittel/
{
  "name": "Äpfel",
  "quantity": 5,
  "einheit": "Stück",
  "kategorie": "Obst",
  "ean_code": "1234567890123",
  "mindestmenge": 3,
  "storage_location_id": 2,
  "package_id": 1,
  "package_count": 1
}

# Search by EAN
GET /api/v1/lebensmittel/ean/1234567890123

# Low stock warnings
GET /api/v1/lebensmittel/warnings/low-stock
```

### 📦 **Transactions**
```http
# Record purchase
POST /api/v1/transactions/
{
  "lebensmittel_id": 1,
  "transaction_type": "purchase",
  "quantity_change": 10,
  "reason": "Weekly shopping"
}

# Record consumption
POST /api/v1/transactions/
{
  "lebensmittel_id": 1,
  "transaction_type": "consumption",
  "quantity_change": 2,
  "reason": "Breakfast"
}
```

## 🎯 Usage Examples

### 📱 **Mobile Workflow**
1. **📷 Scan barcode** → Auto-fills product data
2. **📝 Adjust details** → Quantity, expiration date
3. **💾 Save item** → Added to inventory
4. **🔄 Track usage** → Purchase/consumption buttons
5. **⚠️ Get alerts** → Low stock notifications

### 🌐 **Web Integration**
```javascript
// Example API usage
const response = await fetch('https://yourdomain.com/api/v1/lebensmittel/', {
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  }
});
const items = await response.json();
```

## 🛠️ Development

### 🔧 **Prerequisites**
- **Android Studio** 4.0+ (for Android app)
- **PHP 7.4+** with MySQL (for production backend)
- **Python 3.8+** (for development backend)
- **Git** for version control

### 🚀 **Development Setup**
```bash
# 1. Clone repository
git clone https://github.com/yourusername/FoodTrack.git
cd FoodTrack

# 2. Start Python backend
cd backend/
python -m venv venv
source venv/bin/activate  # or venv\Scripts\activate on Windows
pip install -r requirements.txt
uvicorn src.main:app --reload

# 3. Open Android app in Android Studio
# File → Open → android-client/

# 4. Update API URL in Constants.kt
# const val BASE_URL = "http://10.0.2.2:8000/"  # For emulator
```

### 🧪 **Testing**
```bash
# Backend tests
cd backend/
pytest

# Android tests
cd android-client/
./gradlew test
```

## 🚀 Deployment

### 🌍 **Production Deployment (PHP)**
1. **Upload files** to your hosting provider
2. **Create MySQL database** via hosting control panel
3. **Run setup wizard** at `https://yourdomain.com/setup.php`
4. **Update Android app** with production URL
5. **Delete setup.php** for security

### 🐳 **Docker Deployment (Python)**
```bash
# Build and run with Docker
docker build -t foodtrack-backend backend/
docker run -p 8000:8000 foodtrack-backend
```

## 🤝 Contributing

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📋 Changelog

### 🎉 **v2.1.0 - Multi-Tenant System Release** (Latest)
- ✅ **Complete Multi-Tenant Architecture** with household-based data isolation
- ✅ **Storage Locations & Packages** - Organize items by location and package type
- ✅ **Fixed Backend Issues** - Standalone PHP endpoints without dependencies
- ✅ **Enhanced Android UI** - Material Design 3 with improved navigation
- ✅ **FIFO Batch System** - Advanced inventory management with expiration tracking
- ✅ **Category Management** - Full CRUD operations for food categories
- ✅ **Transaction Tracking** - Complete purchase and consumption history

### 📱 **v2.0.0 - Multi-Tenant Foundation**
- 🏠 Multi-Tenant database schema
- 👥 Household and user management
- 📍 Storage location management
- 📦 Package management system

### 🚀 **v1.x - Initial Release**
- 📱 Android app with barcode scanning
- 🌐 PHP/Python backend options
- 📊 Basic inventory management
- 🏷️ EAN code integration

## 🙏 Acknowledgments

- **OpenFoodFacts** - Product database API
- **Material Design** - UI/UX guidelines
- **ZXing** - Barcode scanning library
- **Retrofit** - HTTP client for Android

## 📞 Support

- **📧 Email:** knut.wehr@gmail.com
- **🐛 Issues:** [GitHub Issues](https://github.com/knuwe1/FoodTrack/issues)
- **📖 Wiki:** [Project Wiki](https://github.com/knuwe1/FoodTrack/wiki)
- **🚀 Releases:** [GitHub Releases](https://github.com/knuwe1/FoodTrack/releases)

---

**Made with ❤️ for better food management and Multi-Tenant architecture**
