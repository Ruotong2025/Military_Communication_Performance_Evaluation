# 同步mysql和chroma的数据 以mysql为主
python scripts/sync_chroma_mysql.py --sync

# 检查mysql和chroma的数据
python scripts/sync_chroma_mysql.py --check-only


python scripts/chroma_manager.py status