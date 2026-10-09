function labelResponsiveTables(root) {
  const tables = new Set();

  if (root instanceof HTMLTableElement) {
    tables.add(root);
  } else {
    if (root instanceof Element) {
      const parentTable = root.closest('table');
      if (parentTable) tables.add(parentTable);
    }
    root.querySelectorAll?.('table').forEach(table => tables.add(table));
  }

  tables.forEach(table => {
    const headerRow = table.tHead?.rows[0];
    if (!headerRow) return;

    const labels = Array.from(headerRow.cells, cell => cell.textContent.trim());
    table.classList.add('responsive-table');

    table.querySelectorAll('tbody tr').forEach(row => {
      Array.from(row.cells).forEach((cell, index) => {
        if (cell.colSpan === 1 && labels[index]) {
          cell.dataset.label = labels[index];
        }
      });
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  labelResponsiveTables(document);

  const observer = new MutationObserver(records => {
    records.forEach(record => {
      record.addedNodes.forEach(node => labelResponsiveTables(node));
    });
  });

  observer.observe(document.body, { childList: true, subtree: true });
});
