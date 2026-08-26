'use strict';

// ─── Map ──────────────────────────────────────────────────────────────────────

const map = L.map('map').setView([-34.6037, -58.3816], 13);

L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
  attribution: '© CARTO',
  maxZoom: 19
}).addTo(map);

// Origin marker (depot) that will be updated by AnyLogic
const iconDepot = L.divIcon({ className: '', html: '<div style="background:#0ea5e9;width:14px;height:14px;border-radius:50%;border:3px solid white;box-shadow:0 0 8px #0ea5e9"></div>', iconSize: [14, 14] });
const markerDepot = L.marker([-34.6037, -58.3816], { icon: iconDepot })
  .addTo(map)
  .bindTooltip('Central Depot (Waiting for AnyLogic...)');

AnyLogic.events.on('setDepot', (coords) => {
  const { lat, lng } = coords;
  markerDepot.setLatLng([lat, lng]).bindTooltip('Depot (AnyLogic)');
  map.setView([lat, lng], 13);
  document.getElementById('inputOrigin').value = `${lat.toFixed(5)}, ${lng.toFixed(5)} (Depot)`;
});

// Selected destination marker
let markerDestination = null;
let coordDestination  = null;

map.on('click', (e) => {
  coordDestination = e.latlng;
  if (markerDestination) markerDestination.setLatLng(e.latlng);
  else markerDestination = L.marker(e.latlng).addTo(map);
  document.getElementById('inputDestination').value = `${e.latlng.lat.toFixed(5)}, ${e.latlng.lng.toFixed(5)}`;
  document.getElementById('mapHint').classList.add('hidden');
});

document.getElementById('inputOrigin').value = '-34.60370, -58.38160 (Depot)';

// ─── Real-time vehicles (simulation markers) ──────────────────────────────────

const vehicles = {};

AnyLogic.events.on('vehicleMoved', (data) => {
  const { id, lat, lng, label } = data;
  const latlng = [lat, lng];
  if (vehicles[id]) {
    vehicles[id].setLatLng(latlng);
  } else {
    const icon = L.divIcon({ className: '', html: `<div style="background:#f59e0b;width:14px;height:14px;border-radius:50%;border:2px solid white"></div>`, iconSize: [14, 14] });
    vehicles[id] = L.marker(latlng, { icon }).addTo(map).bindTooltip(label || ('Truck ' + id));
  }
});

AnyLogic.events.on('orderCompleted', (orderId) => {
  // Mark the order as completed visually and remove the truck from the map
  if (vehicles[orderId]) {
    map.removeLayer(vehicles[orderId]);
    delete vehicles[orderId];
  }
  const idx = listOrders.findIndex(p => p.id === orderId);
  if (idx >= 0) {
    listOrders[idx].priority = 'COMPLETED';
    renderList();
  }
});

// ─── Order Management ─────────────────────────────────────────────────────────

const listOrders = [];

const orders = {
  async createOrder() {
    if (!coordDestination) { alert('Select a destination on the map'); return; }

    const order = {
      destination: document.getElementById('inputDestination').value,
      description: document.getElementById('inputDesc').value || 'No description',
      priority:    document.getElementById('inputPriority').value,
      lat: coordDestination.lat,
      lng: coordDestination.lng,
      timestamp: new Date().toISOString()
    };

      order.id = 'ORD-' + Math.floor(Math.random() * 10000);
      
      try {
        await AnyLogic.call('createOrder', order);
        listOrders.push(order);
        renderList();
        orders.clearForm();
      } catch (e) {
        console.error('Error creating order:', e);
        alert('Error: ' + (e.message || JSON.stringify(e)));
      }
  },

  remove(id) {
    const idx = listOrders.findIndex(p => p.id === id);
    if (idx >= 0) listOrders.splice(idx, 1);
    renderList();
    // Notify AnyLogic
    AnyLogic.call('removeOrder', id).catch(console.error);
  },

  clearForm() {
    document.getElementById('inputDestination').value = '';
    document.getElementById('inputDesc').value    = '';
    document.getElementById('inputPriority').value = 'normal';
    if (markerDestination) { markerDestination.remove(); markerDestination = null; }
    coordDestination = null;
    document.getElementById('mapHint').classList.remove('hidden');
  }
};

function renderList() {
  const container = document.getElementById('listOrders');
  document.getElementById('ordersCount').textContent = `(${listOrders.length})`;
  container.innerHTML = '';

  if (listOrders.length === 0) {
    container.innerHTML = '<p style="color:var(--muted);font-size:.8rem;text-align:center;padding:16px">No orders</p>';
    return;
  }

  listOrders.forEach(p => {
    const el = document.createElement('div');
    el.className = 'pedido-item';
    el.innerHTML = `
      <div class="pedido-info">
        <div class="pedido-id">${p.id || 'Pending'}</div>
        <div class="pedido-dest">${p.destination}</div>
        <div class="pedido-desc">${p.description}</div>
      </div>
      <span class="pedido-badge badge-${p.priority.toLowerCase()}">${p.priority}</span>
      <button class="btn-delete" title="Remove">✕</button>
    `;
    el.querySelector('.btn-delete').onclick = () => orders.remove(p.id);
    container.appendChild(el);
  });
}

// ─── Excel ────────────────────────────────────────────────────────────────────

const excel = {
  async importFile() {
    try {
      const path = await AnyLogic.files.openDialog("Import Orders Excel", "*.xlsx");
      if (!path) return;
      
      const base64 = await AnyLogic.files.read(path, true);
      const wb = XLSX.read(base64, { type: 'base64' });
      const ws = wb.Sheets[wb.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json(ws);
      
      rows.forEach(row => {
        listOrders.push({
          id: row.ID || row.id || ('IMP-' + Date.now()),
          destination: row.Destination || row.destination || '',
          description: row.Description || row.description || '',
          priority: row.Priority || row.priority || 'normal',
          timestamp: row.Timestamp || row.timestamp || new Date().toISOString()
        });
      });
      renderList();
      AnyLogic.call('importOrders', listOrders).catch(console.error);
    } catch(e) {
      console.error(e);
      alert("Error importing the Excel file");
    }
  },

  async exportFile() {
    const data = listOrders.map(p => ({
      ID: p.id, Destination: p.destination, Description: p.description,
      Priority: p.priority, Timestamp: p.timestamp
    }));
    const ws = XLSX.utils.json_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Orders');
    
    // Convert Excel to Base64 using SheetJS
    const base64 = XLSX.write(wb, { bookType: 'xlsx', type: 'base64' });
    
    try {
      const path = await AnyLogic.files.saveDialog("Save Excel", "exported_orders.xlsx");
      if (path) {
        await AnyLogic.files.write(path, base64, true);
        alert("Excel saved successfully to:\n" + path);
      }
    } catch(e) {
      console.error(e);
      alert("Error saving the file");
    }
  }
};

// ─── Init ─────────────────────────────────────────────────────────────────────

window.addEventListener('load', () => {
  renderList();
  // Initialize state from model
  AnyLogic.call('getOrders').then(data => {
    if (Array.isArray(data)) { listOrders.push(...data); renderList(); }
  }).catch(() => { /* model might not have this command on first load */ });
  
  // Notify Java that the UI is fully loaded
  AnyLogic.call('__ready__').catch(e => console.warn(e));
});
