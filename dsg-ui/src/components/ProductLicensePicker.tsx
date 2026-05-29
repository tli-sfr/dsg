import { FormLabel, Radio, RadioGroup } from '@ringcentral/spring-ui';
import type { ProductLicenseId } from '../lib/productLicenses';
import { PRODUCT_LICENSES } from '../lib/productLicenses';

export function ProductLicensePicker({
  value,
  onChange,
  disabled = false,
}: {
  value: ProductLicenseId;
  onChange: (licenseId: ProductLicenseId) => void;
  disabled?: boolean;
}) {
  return (
    <div>
      <FormLabel className="mb-3 typography-descriptorMini uppercase text-neutral-b3">
        User license (select one)
      </FormLabel>
      <RadioGroup
        name="product-license"
        value={value}
        onChange={(e) => onChange(e.target.value as ProductLicenseId)}
      >
        <div className="grid gap-3 sm:grid-cols-3">
          {PRODUCT_LICENSES.map((license) => (
            <label
              key={license.id}
              className={`flex cursor-pointer gap-3 rounded-sui-md border p-4 ${
                value === license.id
                  ? 'border-primary-b bg-neutral-base'
                  : 'border-neutral-b4 bg-neutral-base hover:border-neutral-b3'
              } ${disabled ? 'cursor-not-allowed opacity-60' : ''}`}
            >
              <Radio value={license.id} disabled={disabled} />
              <span>
                <span
                  className={`block typography-labelSemiBold ${
                    value === license.id ? 'text-primary-b' : 'text-neutral-b1'
                  }`}
                >
                  {license.label}
                </span>
                <span className="mt-1 block typography-descriptorMini text-neutral-b3">
                  {license.description}
                </span>
              </span>
            </label>
          ))}
        </div>
      </RadioGroup>
    </div>
  );
}
