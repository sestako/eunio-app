module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/src'],
  testMatch: ['**/__tests__/**/*.ts', '**/?(*.)+(spec|test).ts'],
  transform: {
    '^.+\\.ts$': ['ts-jest', {
      // Use faster transpilation
      tsconfig: {
        target: 'es2018',
        module: 'commonjs',
        skipLibCheck: true,
        skipDefaultLibCheck: true
      }
    }],
  },
  collectCoverageFrom: [
    'src/**/*.ts',
    '!src/**/*.d.ts',
  ],
  setupFilesAfterEnv: ['<rootDir>/src/test/setup.ts'],
  // Performance optimizations
  maxWorkers: 1,
  testTimeout: 3000, // Reduce timeout further
  clearMocks: true,
  resetMocks: true,
  restoreMocks: true,
  // Cache settings for faster subsequent runs
  cacheDirectory: '<rootDir>/.jest-cache',
  // Skip coverage collection for faster runs
  collectCoverage: false,
  // Faster test discovery
  testPathIgnorePatterns: ['/node_modules/', '/dist/'],
  // Disable verbose output for speed
  verbose: false,
  // Use faster resolver
  resolver: undefined,
  // Disable watch mode optimizations that slow down single runs
  watchman: false
};