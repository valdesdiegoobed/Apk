import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.nuevoproyecto.android',
  appName: 'Nuevo Proyecto Android',
  webDir: 'dist',
  android: {
    allowMixedContent: false,
  },
};

export default config;
