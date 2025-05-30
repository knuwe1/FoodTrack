#!/usr/bin/env python3
"""
Migration Script: Konvertiert bestehende Lebensmittel-Daten zum Batch-System

Dieses Script:
1. Sichert die bestehenden Daten
2. Erstellt die neue Batch-Tabelle
3. Migriert bestehende Lebensmittel zu Batches
4. Aktualisiert die Lebensmittel-Tabelle
"""

import sys
import os
from datetime import date, datetime
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

# Erstelle Engine direkt
DATABASE_URL = "sqlite:///./foodtrack.db"
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})

def backup_existing_data(db_session):
    """Erstellt ein Backup der bestehenden Daten"""
    print("📦 Erstelle Backup der bestehenden Daten...")

    # Hole alle bestehenden Lebensmittel
    lebensmittel = db_session.execute(text("""
        SELECT id, name, menge, einheit, ablaufdatum, kategorie
        FROM lebensmittel
    """)).fetchall()

    print(f"   Gefunden: {len(lebensmittel)} Lebensmittel")

    # Speichere in temporärer Tabelle
    db_session.execute(text("""
        CREATE TABLE IF NOT EXISTS lebensmittel_backup AS
        SELECT * FROM lebensmittel
    """))

    db_session.commit()
    print("   ✅ Backup erstellt")
    return lebensmittel

def create_batch_table(db_session):
    """Erstellt die neue Batch-Tabelle"""
    print("🏗️  Erstelle Batch-Tabelle...")

    db_session.execute(text("""
        CREATE TABLE IF NOT EXISTS lebensmittel_batches (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            lebensmittel_id INTEGER NOT NULL,
            menge INTEGER NOT NULL DEFAULT 0,
            ablaufdatum DATE,
            einkaufsdatum DATE NOT NULL DEFAULT CURRENT_DATE,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (lebensmittel_id) REFERENCES lebensmittel (id)
        )
    """))

    db_session.commit()
    print("   ✅ Batch-Tabelle erstellt")

def migrate_lebensmittel_to_batches(db_session, lebensmittel_data):
    """Migriert bestehende Lebensmittel zu Batches"""
    print("🔄 Migriere Lebensmittel zu Batches...")

    migrated_count = 0

    for item in lebensmittel_data:
        id, name, menge, einheit, ablaufdatum, kategorie = item

        # Nur migrieren wenn Menge > 0
        if menge and menge > 0:
            # Erstelle Batch für bestehende Menge
            db_session.execute(text("""
                INSERT INTO lebensmittel_batches
                (lebensmittel_id, menge, ablaufdatum, einkaufsdatum, created_at, updated_at)
                VALUES (:lebensmittel_id, :menge, :ablaufdatum, :einkaufsdatum, :created_at, :updated_at)
            """), {
                'lebensmittel_id': id,
                'menge': menge,
                'ablaufdatum': ablaufdatum,
                'einkaufsdatum': date.today(),
                'created_at': datetime.now(),
                'updated_at': datetime.now()
            })
            migrated_count += 1

    db_session.commit()
    print(f"   ✅ {migrated_count} Lebensmittel zu Batches migriert")

def update_lebensmittel_table(db_session):
    """Entfernt menge und ablaufdatum aus der Lebensmittel-Tabelle"""
    print("🔧 Aktualisiere Lebensmittel-Tabelle...")

    # SQLite unterstützt kein DROP COLUMN, daher erstellen wir eine neue Tabelle
    db_session.execute(text("""
        CREATE TABLE lebensmittel_new (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            einheit TEXT,
            kategorie TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
    """))

    # Kopiere Daten (ohne menge und ablaufdatum)
    db_session.execute(text("""
        INSERT INTO lebensmittel_new (id, name, einheit, kategorie, created_at, updated_at)
        SELECT id, name, einheit, kategorie,
               COALESCE(created_at, CURRENT_TIMESTAMP),
               COALESCE(updated_at, CURRENT_TIMESTAMP)
        FROM lebensmittel
    """))

    # Ersetze alte Tabelle
    db_session.execute(text("DROP TABLE lebensmittel"))
    db_session.execute(text("ALTER TABLE lebensmittel_new RENAME TO lebensmittel"))

    db_session.commit()
    print("   ✅ Lebensmittel-Tabelle aktualisiert")

def update_transactions_table(db_session):
    """Fügt batch_id zur Transactions-Tabelle hinzu"""
    print("🔧 Aktualisiere Transactions-Tabelle...")

    try:
        # Prüfe ob batch_id bereits existiert
        result = db_session.execute(text("PRAGMA table_info(transactions)")).fetchall()
        columns = [row[1] for row in result]

        if 'batch_id' not in columns:
            db_session.execute(text("""
                ALTER TABLE transactions
                ADD COLUMN batch_id INTEGER REFERENCES lebensmittel_batches(id)
            """))
            db_session.commit()
            print("   ✅ batch_id Spalte hinzugefügt")
        else:
            print("   ✅ batch_id Spalte bereits vorhanden")

    except Exception as e:
        print(f"   ⚠️  Warnung bei Transactions-Update: {e}")

def verify_migration(db_session):
    """Verifiziert die Migration"""
    print("🔍 Verifiziere Migration...")

    # Zähle Lebensmittel
    lebensmittel_count = db_session.execute(text("SELECT COUNT(*) FROM lebensmittel")).scalar()

    # Zähle Batches
    batch_count = db_session.execute(text("SELECT COUNT(*) FROM lebensmittel_batches")).scalar()

    # Zähle Gesamtmenge
    total_menge = db_session.execute(text("SELECT SUM(menge) FROM lebensmittel_batches")).scalar() or 0

    print(f"   📊 Lebensmittel: {lebensmittel_count}")
    print(f"   📊 Batches: {batch_count}")
    print(f"   📊 Gesamtmenge: {total_menge}")
    print("   ✅ Migration erfolgreich!")

def main():
    """Hauptfunktion für die Migration"""
    print("🚀 Starte Migration zu Batch-System...")
    print("=" * 50)

    # Erstelle Session
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    db_session = SessionLocal()

    try:
        # 1. Backup erstellen
        lebensmittel_data = backup_existing_data(db_session)

        # 2. Batch-Tabelle erstellen
        create_batch_table(db_session)

        # 3. Daten migrieren
        migrate_lebensmittel_to_batches(db_session, lebensmittel_data)

        # 4. Lebensmittel-Tabelle aktualisieren
        update_lebensmittel_table(db_session)

        # 5. Transactions-Tabelle aktualisieren
        update_transactions_table(db_session)

        # 6. Migration verifizieren
        verify_migration(db_session)

        print("=" * 50)
        print("🎉 Migration erfolgreich abgeschlossen!")
        print("   Das Batch-System ist jetzt aktiv.")
        print("   Backup verfügbar in: lebensmittel_backup")

    except Exception as e:
        print(f"❌ Fehler bei Migration: {e}")
        db_session.rollback()
        return False

    finally:
        db_session.close()

    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
