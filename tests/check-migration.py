#!/usr/bin/env python3
"""Exercise the shipped SQL upgrade against a version-1 ledger with sample data."""
import pathlib,re,sqlite3
root=pathlib.Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/com/ives/money/LedgerDb.java').read_text()
old_schema='''CREATE TABLE entries(id TEXT PRIMARY KEY,stage INTEGER NOT NULL,cents INTEGER NOT NULL,title TEXT NOT NULL,category TEXT,date TEXT,time TEXT,channel TEXT,payment TEXT,type TEXT,note TEXT,txnNo TEXT,imagePath TEXT,sourceKey TEXT,raw TEXT,subscriptionId TEXT,refundOf TEXT,warning TEXT,batchId TEXT,createdAt INTEGER,selected INTEGER)'''
sql=re.search(r'db.execSQL\("(ALTER TABLE entries[^\"]*)"\)',source).group(1)
db=sqlite3.connect(':memory:');db.row_factory=sqlite3.Row;db.execute(old_schema)
for n,stage in [('posted',0),('draft',1)]:
    db.execute('INSERT INTO entries(id,stage,cents,title,date,type,selected,sourceKey) VALUES (?,?,?,?,?,?,?,?)',(n,stage,1250,'测试账单','2026-10-18','支出',1,n+'-source'))
before=[dict(r) for r in db.execute('SELECT * FROM entries ORDER BY id')]
db.execute(sql)
after=[dict(r) for r in db.execute('SELECT * FROM entries ORDER BY id')]
for r in after:assert r.pop('carBrand')==''
assert before==after,'SQL upgrade changed existing entry fields'
assert 'carBrand' in [r['name'] for r in db.execute('PRAGMA table_info(entries)')]
print('PASS ledger v1 to v2 migration preserves posted entries and drafts')
