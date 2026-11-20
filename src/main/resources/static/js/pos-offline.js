(function () {
  const DB_NAME = 'ilonggo_bistro_pos_v1';
  const DB_VERSION = 1;
  const STORE_PENDING = 'pending_orders';
  const STORE_RECEIPTS = 'offline_receipts';

  function openDb() {
    return new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = (e) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains(STORE_PENDING)) {
          db.createObjectStore(STORE_PENDING, { keyPath: 'clientOrderUuid' });
        }
        if (!db.objectStoreNames.contains(STORE_RECEIPTS)) {
          db.createObjectStore(STORE_RECEIPTS, { keyPath: 'offlineReceiptNumber' });
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  async function withStore(storeName, mode, fn) {
    const db = await openDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(storeName, mode);
      const store = tx.objectStore(storeName);
      const result = fn(store);
      tx.oncomplete = () => resolve(result && result.result !== undefined ? result.result : result);
      tx.onerror = () => reject(tx.error);
    });
  }

  function generateUuid() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
      return window.crypto.randomUUID();
    }
    return 'off-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
  }

  function generateOfflineReceiptNumber() {
    const year = new Date().getFullYear();
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let suffix = '';
    for (let i = 0; i < 5; i++) {
      suffix += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return `OR-OFF-${year}-${suffix}`;
  }

  async function queueOfflineOrder(orderRecord) {
    await withStore(STORE_PENDING, 'readwrite', (store) => store.put(orderRecord));
    await withStore(STORE_RECEIPTS, 'readwrite', (store) => store.put(orderRecord));
    return orderRecord;
  }

  async function listPendingOrders() {
    const db = await openDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_PENDING, 'readonly');
      const req = tx.objectStore(STORE_PENDING).getAll();
      req.onsuccess = () => {
        const list = req.result || [];
        list.sort((a, b) => (a.createdAtIso || '').localeCompare(b.createdAtIso || ''));
        resolve(list);
      };
      req.onerror = () => reject(req.error);
    });
  }

  async function listOfflineReceipts() {
    const db = await openDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_RECEIPTS, 'readonly');
      const req = tx.objectStore(STORE_RECEIPTS).getAll();
      req.onsuccess = () => {
        const list = req.result || [];
        list.sort((a, b) => (b.createdAtIso || '').localeCompare(a.createdAtIso || ''));
        resolve(list.slice(0, 50));
      };
      req.onerror = () => reject(req.error);
    });
  }

  async function markReceiptSynced(offlineReceiptNumber, serverReceiptNumber) {
    const db = await openDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_RECEIPTS, 'readwrite');
      const store = tx.objectStore(STORE_RECEIPTS);
      const getReq = store.get(offlineReceiptNumber);
      getReq.onsuccess = () => {
        const rec = getReq.result;
        if (rec) {
          rec.synced = true;
          rec.serverReceiptNumber = serverReceiptNumber;
          store.put(rec);
        }
      };
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  async function removePendingOrder(clientOrderUuid) {
    await withStore(STORE_PENDING, 'readwrite', (store) => store.delete(clientOrderUuid));
  }

  async function fetchFreshCsrf() {
    try {
      const res = await fetch('/admin/pos/csrf-token', { credentials: 'same-origin' });
      if (res.ok) {
        const data = await res.json();
        if (data.token && data.headerName) {
          const tokenMeta = document.querySelector('meta[name="_csrf"]');
          const headerMeta = document.querySelector('meta[name="_csrf_header"]');
          if (tokenMeta) tokenMeta.content = data.token;
          if (headerMeta) headerMeta.content = data.headerName;
          return { headerName: data.headerName, token: data.token };
        }
      }
    } catch (_) {
      // fall back to meta tags
    }
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    return {
      headerName: headerMeta ? headerMeta.content : null,
      token: tokenMeta ? tokenMeta.content : null
    };
  }

  async function syncPendingOrders() {
    const pending = await listPendingOrders();
    if (!pending.length) {
      return { syncedCount: 0, alreadySyncedCount: 0, failedCount: 0, results: [] };
    }

    const csrf = await fetchFreshCsrf();
    const headers = { 'Content-Type': 'application/json' };
    if (csrf.headerName && csrf.token) {
      headers[csrf.headerName] = csrf.token;
    }

    const payload = {
      orders: pending.map((o) => ({
        clientOrderUuid: o.clientOrderUuid,
        offlineReceiptNumber: o.offlineReceiptNumber,
        createdAtIso: o.createdAtIso,
        customerName: o.customerName,
        customerPhone: o.customerPhone,
        orderType: o.orderType,
        tableNumber: o.tableNumber,
        paymentMethod: o.paymentMethod,
        discountType: o.discountType,
        customDiscountPhp: o.customDiscountPhp,
        amountTenderedPhp: o.amountTenderedPhp,
        notes: o.notes,
        items: o.items.map((i) => ({
          menuItemId: i.menuItemId,
          itemName: i.itemName,
          unitPricePhp: i.unitPricePhp,
          quantity: i.quantity
        }))
      }))
    };

    const res = await fetch('/admin/pos/sync', {
      method: 'POST',
      credentials: 'same-origin',
      headers,
      body: JSON.stringify(payload)
    });

    if (res.status === 401 || res.status === 403 || res.redirected) {
      throw new Error('Staff session expired. Sign in again to sync queued offline orders.');
    }
    if (!res.ok) {
      throw new Error('Server rejected sync request (HTTP ' + res.status + ').');
    }

    const data = await res.json();
    for (const item of data.results || []) {
      if (item.status === 'SYNCED' || item.status === 'ALREADY_SYNCED') {
        await removePendingOrder(item.clientOrderUuid);
        await markReceiptSynced(item.offlineReceiptNumber, item.serverReceiptNumber);
      }
    }
    return data;
  }

  function registerServiceWorker() {
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw-pos.js', { scope: '/' }).catch(() => {
        // non-fatal if browser blocks SW
      });
    }
  }

  window.BistroOfflinePos = {
    registerServiceWorker,
    generateUuid,
    generateOfflineReceiptNumber,
    queueOfflineOrder,
    listPendingOrders,
    listOfflineReceipts,
    syncPendingOrders
  };
})();
