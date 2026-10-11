import { userRoles } from '../constants/users'

export const permissions = {
  BROWSE_EVENTS: 'BROWSE_EVENTS',
  CREATE_EVENT: 'CREATE_EVENT',
  MANAGE_OWN_EVENTS: 'MANAGE_OWN_EVENTS',
  ACCESS_ADMIN: 'ACCESS_ADMIN',
  MANAGE_USERS: 'MANAGE_USERS',
  MANAGE_EVENTS: 'MANAGE_EVENTS',
}

const rolePermissions = {
  [userRoles.USER]: [permissions.BROWSE_EVENTS],
  [userRoles.CREATOR]: [permissions.BROWSE_EVENTS, permissions.CREATE_EVENT, permissions.MANAGE_OWN_EVENTS],
  [userRoles.ADMIN]: [
    permissions.BROWSE_EVENTS,
    permissions.CREATE_EVENT,
    permissions.MANAGE_OWN_EVENTS,
    permissions.ACCESS_ADMIN,
    permissions.MANAGE_USERS,
    permissions.MANAGE_EVENTS,
  ],
  [userRoles.LUPPA_ADMIN]: [
    permissions.BROWSE_EVENTS,
    permissions.CREATE_EVENT,
    permissions.MANAGE_OWN_EVENTS,
    permissions.ACCESS_ADMIN,
    permissions.MANAGE_USERS,
    permissions.MANAGE_EVENTS,
  ],
}

export const hasPermission = (user, permission) => {
  if (!user) return false
  return rolePermissions[user.role]?.includes(permission) || false
}

export const hasRole = (user, roles) => {
  if (!user) return false
  return roles.includes(user.role) || (user.role === userRoles.LUPPA_ADMIN && roles.includes(userRoles.ADMIN))
}
