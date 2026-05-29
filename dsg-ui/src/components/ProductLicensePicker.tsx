import type { ProductLicenseId, ProductLicenseOption } from '../lib/productLicenses';
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
      <div className="mb-3 flex items-center gap-2">
        <span className="h-4 w-1 rounded bg-blue-500" aria-hidden />
        <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
          User license (select one)
        </span>
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        {PRODUCT_LICENSES.map((license) => (
          <LicenseCard
            key={license.id}
            license={license}
            selected={value === license.id}
            disabled={disabled}
            onSelect={() => onChange(license.id)}
          />
        ))}
      </div>
    </div>
  );
}

function LicenseCard({
  license,
  selected,
  disabled,
  onSelect,
}: {
  license: ProductLicenseOption;
  selected: boolean;
  disabled: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onSelect}
      className={`relative rounded-xl border-2 px-4 py-4 text-left transition-colors ${
        selected
          ? 'border-blue-600 bg-white'
          : 'border-slate-200 bg-white hover:border-slate-300'
      } disabled:cursor-not-allowed disabled:opacity-60`}
    >
      <span
        className={`absolute right-3 top-3 flex h-5 w-5 items-center justify-center rounded-full border-2 ${
          selected ? 'border-blue-600' : 'border-slate-300'
        }`}
        aria-hidden
      >
        {selected && <span className="h-2.5 w-2.5 rounded-full bg-blue-600" />}
      </span>
      <span className={`block text-sm font-semibold ${selected ? 'text-blue-700' : 'text-slate-900'}`}>
        {license.label}
      </span>
      <span className="mt-1 block text-xs text-slate-500">{license.description}</span>
    </button>
  );
}
