/**
 * Token storage utilities with encryption support.
 * Uses AES encryption for tokens stored in localStorage.
 */

/**
 * Encrypts a string using AES encryption.
 */
async function encrypt(text: string, key: string): Promise<string> {
  const encoder = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    encoder.encode(key),
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );
  
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(12));
  
  const derivedKey = await crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt,
      iterations: 100000,
      hash: 'SHA-256'
    },
    keyMaterial,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt']
  );
  
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    derivedKey,
    encoder.encode(text)
  );
  
  // Combine salt + iv + encrypted data
  const combined = new Uint8Array(salt.length + iv.length + encrypted.byteLength);
  combined.set(salt, 0);
  combined.set(iv, salt.length);
  combined.set(new Uint8Array(encrypted), salt.length + iv.length);
  
  return btoa(String.fromCharCode(...combined));
}

/**
 * Decrypts an encrypted string.
 */
async function decrypt(encryptedData: string, key: string): Promise<string> {
  const combined = Uint8Array.from(atob(encryptedData), c => c.charCodeAt(0));
  
  const salt = combined.slice(0, 16);
  const iv = combined.slice(16, 28);
  const encrypted = combined.slice(28);
  
  const encoder = new TextEncoder();
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    encoder.encode(key),
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );
  
  const derivedKey = await crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt,
      iterations: 100000,
      hash: 'SHA-256'
    },
    keyMaterial,
    { name: 'AES-GCM', length: 256 },
    false,
    ['decrypt']
  );
  
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv },
    derivedKey,
    encrypted
  );
  
  return new TextDecoder().decode(decrypted);
}

/**
 * Gets the encryption key from environment or generates a default.
 * NOTE: In production, this should come from a secure environment variable.
 */
function getEncryptionKey(): string {
  return import.meta.env.VITE_TOKEN_ENCRYPTION_KEY || 'banking-app-default-key-change-in-production';
}

/**
 * Stores encrypted tokens in localStorage.
 */
export async function setTokens(accessToken: string, refreshToken: string): Promise<void> {
  const key = getEncryptionKey();
  const encryptedAccess = await encrypt(accessToken, key);
  const encryptedRefresh = await encrypt(refreshToken, key);
  
  localStorage.setItem('accessToken', encryptedAccess);
  localStorage.setItem('refreshToken', encryptedRefresh);
}

/**
 * Retrieves and decrypts tokens from localStorage.
 */
export async function getTokens(): Promise<{ accessToken: string | null; refreshToken: string | null }> {
  const key = getEncryptionKey();
  
  const encryptedAccess = localStorage.getItem('accessToken');
  const encryptedRefresh = localStorage.getItem('refreshToken');
  
  if (!encryptedAccess || !encryptedRefresh) {
    return { accessToken: null, refreshToken: null };
  }
  
  try {
    const accessToken = await decrypt(encryptedAccess, key);
    const refreshToken = await decrypt(encryptedRefresh, key);
    return { accessToken, refreshToken };
  } catch {
    // Decryption failed - tokens may be corrupted
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    return { accessToken: null, refreshToken: null };
  }
}

/**
 * Clears tokens from localStorage.
 */
export function clearTokens(): void {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

/**
 * Gets just the access token (for use in axios interceptor).
 * Returns null if not found or decryption fails.
 */
export async function getAccessToken(): Promise<string | null> {
  const key = getEncryptionKey();
  const encryptedAccess = localStorage.getItem('accessToken');
  
  if (!encryptedAccess) {
    return null;
  }
  
  try {
    return await decrypt(encryptedAccess, key);
  } catch {
    return null;
  }
}
