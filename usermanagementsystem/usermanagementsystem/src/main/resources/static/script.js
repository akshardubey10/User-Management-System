const baseUrl = '/user';
const $ = id => document.getElementById(id);

let tbody, status, excelInput, userForm, resetBtn;
let currentExcelUserId = null;


function showStatus(msg, isError = false) {
    if (!status) return;
    status.textContent = msg;
    status.style.color = isError ? 'crimson' : 'green';
    setTimeout(() => { status.textContent = ''; }, 3500);
}


async function fetchUsers() {
    try {
        const res = await fetch(baseUrl);
        if (!res.ok) {
            const txt = await res.text().catch(() => '');
            throw new Error('Failed to fetch users: ' + (txt || res.statusText));
        }
        const data = await res.json();
        renderTable(data);
    } catch (e) {
        showStatus('Could not load users. See console.', true);
        console.error(e);
    }
}

function renderTable(list) {
    if (!tbody) return;

    tbody.innerHTML = '';
    if (!Array.isArray(list) || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6">No users</td></tr>`;
        return;
    }

    for (const user of list) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
      <td>${user.id ?? ''}</td>
      <td>${user.name ?? ''}</td>
      <td>${user.phone ?? ''}</td>
      <td>${user.email ?? ''}</td>
      <td class="actions">
        <button data-id="${user.id}" class="edit secondary">Edit</button>
        <button data-id="${user.id}" class="del danger">Delete</button>
      </td>
      <td class="excel-actions">
        <button data-id="${user.id}" class="upload-excel secondary">Upload Excel</button>
        <button data-id="${user.id}" class="download-excel secondary">Download Excel</button>
      </td>
    `;
        tbody.appendChild(tr);
    }


    tbody.querySelectorAll('.edit').forEach(btn =>
        btn.addEventListener('click', onEdit)
    );
    tbody.querySelectorAll('.del').forEach(btn =>
        btn.addEventListener('click', onDelete)
    );
    tbody.querySelectorAll('.upload-excel').forEach(btn =>
        btn.addEventListener('click', onUploadExcelClick)
    );
    tbody.querySelectorAll('.download-excel').forEach(btn =>
        btn.addEventListener('click', onDownloadExcel)
    );
}


async function onEdit(e) {
    const id = e.target.dataset.id;
    try {
        const res = await fetch(`${baseUrl}/${id}`);
        if (!res.ok) throw new Error('Not found');
        const user = await res.json();
        $('userId').value = user.id || '';
        $('name').value = user.name || '';
        $('phone').value = user.phone || '';
        $('email').value = user.email || '';
        showStatus('Loaded user for edit.');
    } catch (err) {
        showStatus('Could not load user', true);
        console.error(err);
    }
}

async function onDelete(e) {
    const id = e.target.dataset.id;
    if (!confirm('Delete user id ' + id + '?')) return;
    try {
        const res = await fetch(`${baseUrl}/${id}`, { method: 'DELETE' });
        const text = await res.text().catch(() => '');
        if (!res.ok) {
            throw new Error(text || 'Delete failed');
        }
        showStatus(text || 'Deleted user.');
        fetchUsers();
    } catch (err) {
        showStatus('Delete failed: ' + err.message, true);
        console.error(err);
    }
}

async function handleFormSubmit(ev) {
    ev.preventDefault();
    const id = $('userId').value.trim();
    const payload = {
        name: $('name').value.trim(),
        phone: $('phone').value.trim(),
        email: $('email').value.trim()
    };

    try {
        let res;
        if (id) {
            res = await fetch(`${baseUrl}/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            res = await fetch(baseUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }

        const text = await res.text().catch(() => '');
        if (!res.ok) {
            throw new Error(text || res.statusText);
        }

        userForm.reset();
        $('userId').value = '';
        showStatus(text || (id ? 'Updated.' : 'Created.'));
        fetchUsers();
    } catch (err) {
        showStatus('Save failed: ' + err.message, true);
        console.error(err);
    }
}


function onUploadExcelClick(e) {
    const id = e.target.dataset.id;
    currentExcelUserId = id;
    excelInput.value = '';
    excelInput.click();
}

async function handleExcelChange() {
    const file = excelInput.files[0];
    if (!file || !currentExcelUserId) return;

    if (!file.name.toLowerCase().endsWith('.xlsx')) {
        showStatus('Please select a .xlsx file.', true);
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
        const res = await fetch(`${baseUrl}/${currentExcelUserId}/excel`, {
            method: 'POST',
            body: formData
        });
        const text = await res.text().catch(() => '');
        if (!res.ok) throw new Error(text || 'Upload failed');

        showStatus(text || 'Excel uploaded successfully.');
    } catch (err) {
        showStatus('Excel upload failed: ' + err.message, true);
        console.error(err);
    } finally {
        currentExcelUserId = null;
    }
}


async function onDownloadExcel(e) {
    const id = e.target.dataset.id;

    try {
        const res = await fetch(`${baseUrl}/${id}/excel`, {
            method: 'GET',
            headers: {
                'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
            }
        });

        if (!res.ok) {
            const txt = await res.text().catch(() => '');
            throw new Error(txt || 'Download failed');
        }

        const contentType = res.headers.get('content-type') || '';

        if (
            !contentType.includes('spreadsheet') &&
            !contentType.includes('excel') &&
            !contentType.includes('octet-stream')
        ) {
             console.warn("Unexpected content type:", contentType);
        }

        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `user-${id}-scores.xlsx`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);

        showStatus('Excel downloaded.');
    } catch (err) {
        showStatus('Excel download failed: ' + err.message, true);
        console.error(err);
    }
}


document.addEventListener('DOMContentLoaded', () => {

    tbody = document.querySelector('#usersTable tbody');
    status = document.getElementById('status');
    excelInput = document.getElementById('excelInput');
    userForm = document.getElementById('userForm');
    resetBtn = document.getElementById('resetBtn');


    if(userForm) userForm.addEventListener('submit', handleFormSubmit);

    if(resetBtn) resetBtn.addEventListener('click', () => {
        userForm.reset();
        $('userId').value = '';
    });

    if(excelInput) excelInput.addEventListener('change', handleExcelChange);


    fetchUsers();
});