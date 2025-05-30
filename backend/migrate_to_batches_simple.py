#!/usr/bin/env python3
"""
Vereinfachte Migration zu Batch-System
"""

import sqlite3
from datetime import date, datetime

def migrate_to_batches():
    """Migriert die bestehende Datenbank zum Batch-System"""
    
    print("🚀 Starte Migration zu Batch-System...")
    
    # Verbinde zur Datenbank
    conn = sqlite3.connect('test.db')
    cursor = conn.cursor()
    
    try:
        # 1. Erstelle Batch-Tabelle
        print("📦 Erstelle Batch-Tabelle...")
        cursor.execute("""
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
        """)
        
        # 2. Hole alle Lebensmittel mit aktueller Menge
        print("🔄 Migriere bestehende Lebensmittel...")
        cursor.execute("SELECT id, name, menge, ablaufdatum FROM lebensmittel WHERE menge > 0")
        lebensmittel = cursor.fetchall()
        
        # 3. Erstelle für jedes Lebensmittel eine Batch mit der aktuellen Menge
        for lm_id, name, menge, ablaufdatum in lebensmittel:
            if menge and menge > 0:
                cursor.execute("""
                    INSERT INTO lebensmittel_batches 
                    (lebensmittel_id, menge, ablaufdatum, einkaufsdatum, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                """, (
                    lm_id, 
                    menge, 
                    ablaufdatum, 
                    date.today().isoformat(),
                    datetime.now().isoformat(),
                    datetime.now().isoformat()
                ))
                print(f"   ✅ {name}: {menge} Stück (MHD: {ablaufdatum or 'kein'})")
        
        # 4. Füge batch_id zur transactions Tabelle hinzu (falls nicht vorhanden)
        print("🔧 Erweitere Transactions-Tabelle...")
        try:
            cursor.execute("ALTER TABLE transactions ADD COLUMN batch_id INTEGER REFERENCES lebensmittel_batches(id)")
        except sqlite3.OperationalError as e:
            if "duplicate column name" in str(e):
                print("   ✅ batch_id Spalte bereits vorhanden")
            else:
                print(f"   ⚠️  Warnung: {e}")
        
        # 5. Setze Mengen in lebensmittel auf 0 (werden jetzt über Batches verwaltet)
        print("🔄 Aktualisiere Lebensmittel-Tabelle...")
        cursor.execute("UPDATE lebensmittel SET menge = 0")
        
        # 6. Verifiziere Migration
        cursor.execute("SELECT COUNT(*) FROM lebensmittel_batches")
        batch_count = cursor.fetchone()[0]
        
        cursor.execute("SELECT SUM(menge) FROM lebensmittel_batches")
        total_menge = cursor.fetchone()[0] or 0
        
        print(f"\n📊 Migration abgeschlossen:")
        print(f"   📦 Batches erstellt: {batch_count}")
        print(f"   📊 Gesamtmenge: {total_menge}")
        
        conn.commit()
        print("✅ Migration erfolgreich!")
        return True
        
    except Exception as e:
        print(f"❌ Fehler bei Migration: {e}")
        conn.rollback()
        return False
        
    finally:
        conn.close()

if __name__ == "__main__":
    success = migrate_to_batches()
    if success:
        print("\n🎉 Batch-System ist jetzt aktiv!")
        print("   Jeder Einkauf erstellt eine separate Batch mit eigenem MHD.")
        print("   Verbrauch erfolgt automatisch nach FIFO-Prinzip.")
    else:
        print("\n💥 Migration fehlgeschlagen!")
