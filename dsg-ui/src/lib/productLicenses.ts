export type ProductLicenseId = 'VideoPro' | 'VideoProPlus' | 'RingEX';

export type ProductLicenseOption = {
  id: ProductLicenseId;
  label: string;
  description: string;
};

export const PRODUCT_LICENSES: ProductLicenseOption[] = [
  { id: 'VideoPro', label: 'Video Pro', description: 'Meetings only' },
  { id: 'VideoProPlus', label: 'Video Pro+', description: 'Advanced Meetings' },
  { id: 'RingEX', label: 'RingEX', description: 'Core phone & messaging' },
];
