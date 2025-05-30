# 🚀 FoodTrack Quick Setup für .env

## ⚡ Schnell-Installation (5 Minuten)

### **1. Dateien hochladen**
- Alle Dateien per FTP in dein 1blu Hauptverzeichnis

### **2. Datenbank vorbereiten**
- 1blu Control Panel → MySQL-Datenbank erstellen
- phpMyAdmin → SQL-Tab → `database/setup.sql` ausführen

### **3. Setup-Script verwenden**
1. **Browser öffnen:** `https://DEINE-DOMAIN.de/setup.php`
2. **Formular ausfüllen:**
   - Database Host: `localhost`
   - Database Name: `db123456_foodtrack` (dein DB-Name)
   - Database User: `db123456_user` (dein DB-User)
   - Database Password: `dein-passwort`
   - JWT Secret: (leer lassen für Auto-Generation)
3. **"Create .env File"** klicken
4. **Test erfolgreich** → `setup.php` löschen

### **4. Android-App konfigurieren**
- API-URL ändern zu: `https://DEINE-DOMAIN.de/api/v1/`

## ✅ Fertig!

**Login-Daten:**
- Email: `admin@foodtrack.com`
- Passwort: `admin`

## 🔧 Manuelle .env Erstellung (Alternative)

```bash
# .env.example kopieren
cp .env.example .env

# .env bearbeiten
nano .env
```

**Inhalt:**
```env
DB_HOST=localhost
DB_NAME=db123456_foodtrack
DB_USER=db123456_user
DB_PASS="MeinPasswort123!"
JWT_SECRET=AbCdEf123456789RandomKey
APP_DEBUG=false
```

**Berechtigung setzen:**
```bash
chmod 600 .env
```

## 🧪 Testen

1. **API-Test:** `https://DEINE-DOMAIN.de/api/v1/lebensmittel/`
2. **Login-Test:** Android-App mit obigen Credentials

## 🔐 Sicherheit

- ✅ `.env` wird nicht in Git committet
- ✅ Sichere Dateiberechtigungen (600)
- ✅ JWT-Secret automatisch generiert
- ✅ Setup-Script nach Installation löschen

## 📞 Probleme?

**Häufige Fehler:**
- **"Database connection failed"** → DB-Credentials prüfen
- **"Configuration missing"** → .env Datei erstellen
- **"Permission denied"** → Dateiberechtigungen prüfen (`chmod 600 .env`)
- **"Tables not found"** → SQL-Setup ausführen

**Debug aktivieren:**
```env
APP_DEBUG=true
```
