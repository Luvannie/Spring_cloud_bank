import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { 
  LayoutDashboard, 
  Wallet, 
  ArrowRightLeft, 
  CreditCard, 
  LogOut,
  Menu,
  X,
  Bell
} from 'lucide-react'
import { useState } from 'react'
import { cn } from '@/utils'

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'Accounts', href: '/accounts', icon: Wallet },
  { name: 'Transfers', href: '/transfers', icon: ArrowRightLeft },
  { name: 'Payments', href: '/payments', icon: CreditCard },
]

export function Layout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top Navigation */}
      <nav className="bg-white shadow-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            {/* Logo & Desktop Nav */}
            <div className="flex">
              <div className="flex-shrink-0 flex items-center">
                <span className="text-xl font-bold text-primary-600">🏦 Banking</span>
              </div>
              
              {/* Desktop Navigation */}
              <div className="hidden sm:ml-8 sm:flex sm:space-x-4">
                {navigation.map((item) => {
                  const Icon = item.icon
                  const isActive = location.pathname === item.href
                  return (
                    <Link
                      key={item.name}
                      to={item.href}
                      className={cn(
                        'inline-flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors',
                        isActive
                          ? 'bg-primary-50 text-primary-600'
                          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                      )}
                    >
                      <Icon className="h-4 w-4 mr-2" />
                      {item.name}
                    </Link>
                  )
                })}
              </div>
            </div>

            {/* Right side */}
            <div className="flex items-center space-x-4">
              {/* Notifications */}
              <button className="p-2 text-gray-400 hover:text-gray-600 relative">
                <Bell className="h-5 w-5" />
                <span className="absolute top-1 right-1 h-2 w-2 bg-red-500 rounded-full"></span>
              </button>

              {/* User menu */}
              <div className="hidden sm:flex items-center space-x-3">
                <span className="text-sm text-gray-700">{user?.username}</span>
                <button
                  onClick={handleLogout}
                  className="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-md transition-colors"
                >
                  <LogOut className="h-4 w-4 mr-1" />
                  Logout
                </button>
              </div>

              {/* Mobile menu button */}
              <button
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className="sm:hidden p-2 text-gray-400 hover:text-gray-600"
              >
                {isMobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile menu */}
        {isMobileMenuOpen && (
          <div className="sm:hidden border-t border-gray-200">
            <div className="px-4 py-3 space-y-1">
              {navigation.map((item) => {
                const Icon = item.icon
                const isActive = location.pathname === item.href
                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    onClick={() => setIsMobileMenuOpen(false)}
                    className={cn(
                      'block px-3 py-2 text-base font-medium rounded-md transition-colors',
                      isActive
                        ? 'bg-primary-50 text-primary-600'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                    )}
                  >
                    <Icon className="h-5 w-5 inline mr-2" />
                    {item.name}
                  </Link>
                )
              })}
              <button
                onClick={handleLogout}
                className="block w-full text-left px-3 py-2 text-base font-medium text-gray-600 hover:bg-gray-50 rounded-md"
              >
                <LogOut className="h-5 w-5 inline mr-2" />
                Logout
              </button>
            </div>
          </div>
        )}
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}
