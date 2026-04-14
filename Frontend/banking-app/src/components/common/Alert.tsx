import { AlertTriangle, CheckCircle, Info, XCircle, X } from 'lucide-react'
import { cn } from '@/utils'

interface AlertProps {
  children: React.ReactNode
  variant?: 'success' | 'warning' | 'danger' | 'info'
  title?: string
  onClose?: () => void
  className?: string
}

const variantConfig = {
  success: {
    classes: 'bg-green-50 border-green-200 text-green-800',
    icon: CheckCircle,
    iconColor: 'text-green-600',
  },
  warning: {
    classes: 'bg-yellow-50 border-yellow-200 text-yellow-800',
    icon: AlertTriangle,
    iconColor: 'text-yellow-600',
  },
  danger: {
    classes: 'bg-red-50 border-red-200 text-red-800',
    icon: XCircle,
    iconColor: 'text-red-600',
  },
  info: {
    classes: 'bg-blue-50 border-blue-200 text-blue-800',
    icon: Info,
    iconColor: 'text-blue-600',
  },
}

export function Alert({ children, variant = 'info', title, onClose, className }: AlertProps) {
  const config = variantConfig[variant]
  const Icon = config.icon

  return (
    <div className={cn('border rounded-lg p-4', config.classes, className)}>
      <div className="flex items-start">
        <Icon className={cn('h-5 w-5 mr-3 mt-0.5', config.iconColor)} />
        <div className="flex-1">
          {title && <h4 className="font-medium mb-1">{title}</h4>}
          <div className="text-sm">{children}</div>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            className="ml-3 hover:opacity-70 transition-opacity"
            aria-label="Close"
          >
            <X className="h-5 w-5" />
          </button>
        )}
      </div>
    </div>
  )
}
