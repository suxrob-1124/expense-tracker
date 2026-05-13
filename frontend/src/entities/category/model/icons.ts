import {
  Utensils,
  Car,
  Home,
  ShoppingCart,
  HeartPulse,
  Plane,
  Gift,
  Coffee,
  Briefcase,
  GraduationCap,
  Wallet,
  Package,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

/** All valid category icon identifiers supported by the application. */
export const CATEGORY_ICONS: string[] = [
  'Utensils',
  'Car',
  'Home',
  'ShoppingCart',
  'HeartPulse',
  'Plane',
  'Gift',
  'Coffee',
  'Briefcase',
  'GraduationCap',
  'Wallet',
  'Package',
]

/** Map from a {@link CATEGORY_ICONS} identifier to the corresponding Lucide icon component. */
export const ICON_MAP: Record<string, LucideIcon> = {
  Utensils,
  Car,
  Home,
  ShoppingCart,
  HeartPulse,
  Plane,
  Gift,
  Coffee,
  Briefcase,
  GraduationCap,
  Wallet,
  Package,
}
