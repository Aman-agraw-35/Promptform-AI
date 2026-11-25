"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import type { AuthUser } from "@/features/auth/lib/auth-api";

const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export function ProfileMenu() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let active = true;
    fetch(`${apiUrl}/api/auth/me`, { credentials: "include" })
      .then((response) => (response.ok ? response.json() as Promise<AuthUser> : null))
      .then((currentUser) => { if (active) setUser(currentUser); })
      .catch(() => { if (active) setUser(null); })
      .finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) setIsOpen(false);
    }
    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, []);

  async function handleLogout() {
    await fetch(`${apiUrl}/api/auth/logout`, { method: "POST", credentials: "include" });
    setUser(null);
    setIsOpen(false);
  }

  if (isLoading) return <span className="profile-loading" aria-label="Loading account" />;
  if (!user) return <Link className="topbar-evals" href="/login">sign in ↗</Link>;

  return (
    <div className="profile-menu" ref={menuRef}>
      <button className="profile-button" onClick={() => setIsOpen((open) => !open)} type="button" aria-expanded={isOpen}>
        <span className="profile-avatar" aria-hidden="true">{user.displayName.charAt(0).toUpperCase()}</span>
        <span>{user.displayName}</span>
        <span className="profile-chevron" aria-hidden="true">⌄</span>
      </button>
      {isOpen ? (
        <div className="profile-popover" role="dialog" aria-label="Profile">
          <p className="profile-popover-label">Your account</p>
          <strong>{user.displayName}</strong>
          <span>{user.email}</span>
          <button className="profile-logout" onClick={() => void handleLogout()} type="button">Log out</button>
        </div>
      ) : null}
    </div>
  );
}
