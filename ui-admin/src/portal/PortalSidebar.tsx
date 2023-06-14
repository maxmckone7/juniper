import React from 'react'
import { usersPath } from './PortalRouter'
import { SidebarNavLink } from '../navbar/AdminNavbar'

// TODO: Add JSDoc
// eslint-disable-next-line jsdoc/require-jsdoc
const PortalSidebar = ({ portalShortcode }: {portalShortcode: string}) => {
  return <ul className="nav nav-pills flex-column mb-auto">
    <li>
      <SidebarNavLink to={usersPath(portalShortcode)}>{portalShortcode} users</SidebarNavLink>
    </li>
  </ul>
}

export default PortalSidebar
