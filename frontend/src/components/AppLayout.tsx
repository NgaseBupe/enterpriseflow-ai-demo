import { NavLink, Outlet } from 'react-router'
import styles from './AppLayout.module.css'

export function AppLayout() {
  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <span className={styles.brand}>EnterpriseFlow AI</span>
          <nav aria-label="Main">
            <NavLink to="/documents" end className={({ isActive }) => (isActive ? styles.activeLink : styles.link)}>
              Documents
            </NavLink>
            <NavLink to="/documents/upload" className={({ isActive }) => (isActive ? styles.activeLink : styles.link)}>
              Upload
            </NavLink>
          </nav>
        </div>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </div>
  )
}
