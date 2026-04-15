import { useState } from 'react'
import { Card, CardContent, Button, Alert } from '@/components/common'
import { CreditCard, QrCode, ExternalLink, Copy, CheckCircle } from 'lucide-react'
import type { PaymentLinkResponse } from '@/types'

export function PaymentsPage() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [paymentLink, setPaymentLink] = useState<PaymentLinkResponse | null>(null)
  const [copied, setCopied] = useState(false)

  // Demo form state
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')

  const handleCreatePayment = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setPaymentLink(null)

    if (!amount || Number(amount) <= 0) {
      setError('Please enter a valid amount')
      return
    }

    setIsLoading(true)
    try {
      // Simulate API call for demo
      // In real app: const response = await paymentApi.createPaymentLink({ ... })
      await new Promise((resolve) => setTimeout(resolve, 1000))
      
      // Mock response
      setPaymentLink({
        paymentId: `PAY-${Date.now()}`,
        checkoutUrl: 'https://pay.payos.vn/checkout/abc123',
        qrCodeUrl: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
        expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create payment link')
    } finally {
      setIsLoading(false)
    }
  }

  const handleCopyLink = () => {
    if (paymentLink?.checkoutUrl) {
      navigator.clipboard.writeText(paymentLink.checkoutUrl)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
        <p className="text-gray-500 mt-1">Create payment links and QR codes</p>
      </div>

      {/* Error Alert */}
      {error && (
        <Alert variant="danger" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Create Payment Link */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
              <CreditCard className="h-5 w-5 mr-2 text-primary-600" />
              Create Payment Link
            </h3>

            <form onSubmit={handleCreatePayment} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Amount (VND)
                </label>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="100000"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description
                </label>
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Payment for..."
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                />
              </div>

              <Button type="submit" isLoading={isLoading} className="w-full">
                Generate Payment Link
              </Button>
            </form>
          </CardContent>
        </Card>

        {/* Payment Link Result */}
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
              <QrCode className="h-5 w-5 mr-2 text-primary-600" />
              Payment Details
            </h3>

            {paymentLink ? (
              <div className="space-y-4">
                {/* QR Code placeholder */}
                <div className="flex justify-center p-4 bg-gray-50 rounded-lg">
                  <div className="bg-white p-4 rounded-lg shadow-sm">
                    {/* Actual QR would be rendered here */}
                    <QrCode className="h-32 w-32 text-gray-400" />
                  </div>
                </div>

                {/* Payment ID */}
                <div className="p-3 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-500">Payment ID</p>
                  <p className="font-mono font-medium">{paymentLink.paymentId}</p>
                </div>

                {/* Checkout URL */}
                <div className="p-3 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-500 mb-2">Checkout URL</p>
                  <div className="flex items-center space-x-2">
                    <input
                      type="text"
                      readOnly
                      value={paymentLink.checkoutUrl}
                      className="flex-1 px-3 py-2 bg-white border rounded text-sm font-mono"
                    />
                    <Button variant="outline" size="sm" onClick={handleCopyLink}>
                      {copied ? <CheckCircle className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                    </Button>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex space-x-3">
                  <Button
                    variant="outline"
                    className="flex-1"
                    onClick={() => window.open(paymentLink.checkoutUrl, '_blank')}
                  >
                    <ExternalLink className="h-4 w-4 mr-2" />
                    Open Payment Page
                  </Button>
                </div>

                <p className="text-xs text-gray-400 text-center">
                  This payment link expires in 15 minutes
                </p>
              </div>
            ) : (
              <div className="text-center py-8">
                <QrCode className="h-16 w-16 text-gray-200 mx-auto mb-3" />
                <p className="text-gray-500">
                  Create a payment link to see details here
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Info Card */}
      <Card className="bg-blue-50 border-blue-100">
        <CardContent>
          <h4 className="font-medium text-blue-900 mb-2">💡 About PayOS Payments</h4>
          <ul className="text-sm text-blue-800 space-y-1">
            <li>• Payment links are valid for 15 minutes</li>
            <li>• QR codes can be scanned with any banking app</li>
            <li>• You will receive instant notifications on payment completion</li>
            <li>• Supported banks: Vietcombank, ACB, VietinBank, and 40+ others</li>
          </ul>
        </CardContent>
      </Card>
    </div>
  )
}
