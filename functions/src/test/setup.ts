// Minimal mocks for maximum performance

// Simplified Firebase Admin mock
jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  firestore: jest.fn(() => ({
    collection: jest.fn(() => ({
      doc: jest.fn(() => ({
        get: jest.fn().mockResolvedValue({ exists: false }),
        collection: jest.fn(() => ({
          where: jest.fn(() => ({
            orderBy: jest.fn(() => ({
              get: jest.fn().mockResolvedValue({ docs: [] })
            }))
          })),
          doc: jest.fn(() => ({ id: 'mock-id' }))
        }))
      })),
      get: jest.fn().mockResolvedValue({ docs: [], size: 0 })
    })),
    batch: jest.fn(() => ({
      set: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
      commit: jest.fn().mockResolvedValue(undefined)
    }))
  })),
  apps: []
}));

// Simplified Firebase Functions mock
jest.mock('firebase-functions', () => ({
  logger: {
    info: jest.fn(),
    error: jest.fn(),
    warn: jest.fn(),
    debug: jest.fn()
  },
  pubsub: {
    schedule: jest.fn(() => ({
      timeZone: jest.fn(() => ({
        onRun: jest.fn()
      }))
    }))
  },
  https: {
    onCall: jest.fn(),
    HttpsError: class HttpsError extends Error {
      constructor(public code: string, message: string) {
        super(message);
      }
    }
  }
}));

// Global test utilities
global.console = {
  ...console,
  // Suppress console.log during tests
  log: jest.fn(),
  debug: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
  error: jest.fn()
};