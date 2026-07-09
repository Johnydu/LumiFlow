# CSS Folder Structure - LumiFlow

## Overview

The CSS has been reorganized from a monolithic 389-line file into a modular, well-organized structure across multiple files and folders. This improves maintainability, readability, and makes updates easier.

## Folder Structure

```
css/
├── variables.css           # CSS custom properties (colors, sizes, etc.)
├── globals.css             # Universal reset and body baseline styles
├── sidebar.css             # Sidebar component used across pages
├── topbar.css              # Top navigation bar component
├── responsive.css          # Media queries for responsive design
├── utils.css               # Utility classes (color utilities, etc.)
├── CSS_IMPORT_ORDER.txt    # Import sequence documentation
├── README.md               # This file
├── components/             # Reusable component styles
│   ├── buttons.css         # All button styles (.btn, .btn-edit, etc.)
│   ├── forms.css           # Form components (.form-panel, .field, etc.)
│   ├── tables.css          # Table styles (th, td, .table-progress, etc.)
│   ├── cards.css           # Card components (.card, .panel, .info-card, etc.)
│   ├── badges.css          # Badge/tag styles (.badge, .setor-tag, etc.)
│   ├── status.css          # Status indicators (.status, .s-dot, etc.)
│   └── alerts.css          # Alert messages (.alert-success, .alert-error, etc.)
└── pages/                  # Page-specific styles
    ├── dashboard.css       # Dashboard page (body.page-dashboard-dashboard)
    ├── login.css           # Login page (body.page-usuario-login)
    ├── usuarios.css        # Users management page
    ├── maquinas.css        # Machines page
    ├── produtos.css        # Products page
    ├── setores.css         # Sectors page
    ├── ordens.css          # Order details page
    ├── relatorio.css       # Reports page
    ├── vidracaria.css      # Glass department page
    ├── novaordem.css       # New order page
    └── listaordem.css      # Order list page
```

## File Organization Principles

### 1. Variables (`variables.css`)
- **Purpose**: Centralized CSS custom properties
- **Contents**: Color schemes, spacing, borders, shadows
- **Benefit**: Change colors/sizes globally by updating one file
- **Import Order**: FIRST - all files depend on these

### 2. Globals (`globals.css`)
- **Purpose**: Universal styles and base setup
- **Contents**: Universal reset (*), body baseline, page-level defaults
- **Benefit**: Consistent baseline across all pages
- **Import Order**: SECOND - before components

### 3. Sidebar & Topbar (`sidebar.css`, `topbar.css`)
- **Purpose**: Shared layout components used across multiple pages
- **Contents**: Navigation, headers, layout structures
- **Benefit**: Multi-page variants defined once without duplication
- **Import Order**: After globals, before page-specific styles

### 4. Components (`components/` folder)
- **Purpose**: Reusable UI components (buttons, forms, cards, etc.)
- **Organization**: Each component type gets its own file
  - `buttons.css`: `.btn`, `.btn-edit`, `.btn-delete`, `.btn-submit`, etc.
  - `forms.css`: `.form-panel`, `.field`, `.form-actions`, `.setor-check`, etc.
  - `tables.css`: `table`, `th`, `td`, `.table-progress`, `.user-info`, etc.
  - `cards.css`: `.card`, `.panel`, `.info-card`, `.saldo-card`, `.kpi-grid`, etc.
  - `badges.css`: `.badge`, `.setor-tag`, `.nivel-tag`, `.etapa-tag`, etc.
  - `status.css`: `.status`, `.status-purple`, `.s-dot`, etc.
  - `alerts.css`: `.alert-success`, `.alert-error`, `.msg`, etc.
- **Benefit**: Easy to find component styles, promote reuse, minimize duplication
- **Import Order**: Before page-specific styles

### 5. Pages (`pages/` folder)
- **Purpose**: Page-specific style overrides and additional styles
- **Pattern**: All styles scoped under `body.page-[module]-[page]` selector
- **Pages**: Each page gets its own file (dashboard, login, usuarios, etc.)
- **Benefit**: Page-specific variants override component defaults without affecting other pages
- **Import Order**: After components

### 6. Utils (`utils.css`)
- **Purpose**: Utility classes for quick, one-off styling
- **Contents**: Color utilities (`.orange`, `.green`, `.red`) and page-specific partial styles
- **Benefit**: Apply styles without modifying component/page files
- **Import Order**: Near end, before responsive

### 7. Responsive (`responsive.css`)
- **Purpose**: Media queries for mobile/tablet layouts
- **Breakpoints**: `@media (max-width: 1200px)` and `@media (max-width: 900px)`
- **Scoping**: Breakpoints scoped by page selector (e.g., `body.page-maquina-cadastromaquinas`)
- **Benefit**: All responsive styles in one place, easy to find
- **Import Order**: LAST - overrides all other styles

## How to Add New Styles

### Add a New Component Style
1. Create a new file in `css/components/` (e.g., `modals.css`)
2. Add component styles (e.g., `.modal`, `.modal-header`, `.modal-body`)
3. Add import to your HTML (maintain import order from `CSS_IMPORT_ORDER.txt`)

### Add Page-Specific Styling
1. Edit the corresponding file in `css/pages/` (e.g., `dashboard.css`)
2. Scope styles under `body.page-[module]-[page]` selector
3. Keep page-specific overrides close to component definitions

### Update Global Variables
1. Edit `css/variables.css`
2. Add new CSS custom properties to appropriate section (colors, sizes, etc.)
3. All files automatically use new variables

### Add New Page
1. Create `css/pages/[pagename].css`
2. Scope all styles under `body.page-[module]-[page]`
3. Add import to HTML following `CSS_IMPORT_ORDER.txt`
4. Add media queries to `css/responsive.css` under new page section

## Naming Conventions

### CSS Variables
- Color: `--color-[name]` (e.g., `--color-primary`, `--color-success`)
- Size: `--size-[name]` or `--gap-[name]` (e.g., `--size-sm`, `--gap-md`)
- Other: `--[type]-[name]` (e.g., `--border-radius-lg`, `--shadow-default`)

### CSS Classes (Page-Agnostic)
- Components: `.btn`, `.form-panel`, `.card`, `.badge`, `.status`
- Modifiers: `.btn-primary`, `.card-large`, `.status-success`
- Elements: `.btn-text`, `.card-header`, `.form-label`

### Page-Specific Classes
- Page wrapper: `body.page-[module]-[page]` (e.g., `body.page-maquina-cadastromaquinas`)
- Page elements inherit component classes but may have page-specific adjustments

## CSS Cascade & Specificity

The modular structure carefully manages CSS specificity:

1. **Low specificity**: Variables and globals
2. **Medium specificity**: Component base styles (`.btn`, `.card`, etc.)
3. **Higher specificity**: Component modifiers (`.btn-primary`, `.card-large`)
4. **Highest specificity**: Page-specific overrides (`body.page-* .btn`)

This allows:
- Component defaults work globally
- Pages override defaults when needed
- Utilities apply selectively without breaking components

## Maintenance Tips

### Reducing Duplicate Styles
If you find the same style in multiple page files:
1. Check if it's truly a global component style → move to `components/`
2. Check if it's a sidebar/topbar variant → check `sidebar.css`/`topbar.css`
3. If page-specific, consider if consolidation is worth the added complexity

### Finding Styles
- Looking for a color? Check `variables.css`
- Looking for button styles? Check `components/buttons.css`
- Looking for page-specific dashboard styles? Check `pages/dashboard.css`
- Looking for mobile layout? Check `responsive.css`

### Making Changes Safely
1. Always check current specificity - search for selector across all files
2. Update variables before changing component/page files
3. Test responsive breakpoints after changes
4. Use page-specific selectors (`body.page-*`) to avoid unintended side effects

## Import Sequence Validation

**Current Import Order** (see `CSS_IMPORT_ORDER.txt` for full details):
1. variables.css
2. globals.css
3. sidebar.css
4. topbar.css
5. components/buttons.css
6. components/forms.css
7. components/tables.css
8. components/cards.css
9. components/badges.css
10. components/status.css
11. components/alerts.css
12. pages/dashboard.css
13. pages/login.css
14. pages/usuarios.css
15. pages/maquinas.css
16. pages/produtos.css
17. pages/setores.css
18. pages/ordens.css
19. pages/relatorio.css
20. pages/vidracaria.css
21. pages/novaordem.css
22. pages/listaordem.css
23. utils.css
24. responsive.css

**Critical**: Maintain this order in your HTML. Variables must come first, responsive last.

## Original File Reference

The original monolithic `style.css` file contained:
- 626 CSS rules (opening braces)
- 364 non-empty lines of CSS
- All styles mixed without organization

The reorganized structure maintains 100% of original styles across dedicated files, improving:
- **Maintainability**: Find and update styles faster
- **Reusability**: Component styles work across pages without duplication
- **Scalability**: Add new pages/components without cluttering existing files
- **Readability**: Clear separation of concerns

All original functionality is preserved with zero style loss.
