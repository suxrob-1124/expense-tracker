import { NextRequest, NextResponse } from 'next/server'

const PROTECTED = ['/dashboard', '/transactions']
const AUTH_ROUTES = ['/login', '/register']

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl
  // accessToken cookie path is '/dashboard', so it is only sent by the browser
  // on requests to /dashboard and its sub-paths — which is exactly where we need it.
  const accessToken = request.cookies.get('accessToken')?.value

  const isProtected = PROTECTED.some((p) => pathname === p || pathname.startsWith(p + '/'))
  const isAuthRoute = AUTH_ROUTES.some((p) => pathname === p || pathname.startsWith(p + '/'))

  if (isProtected && !accessToken) {
    return NextResponse.redirect(new URL('/login', request.url))
  }

  if (isAuthRoute && accessToken) {
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
}
