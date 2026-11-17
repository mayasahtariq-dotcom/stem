import { expect, type Page } from "@playwright/test";

export const DEMO = { student: "9876543210", teacher: "9876500001", parent: "9876500002", key: "BI09-K4X2" };

export async function chooseLanguageAndRole(page: Page, language: RegExp, role: RegExp) {
  await page.goto("/");
  await page.waitForURL("**/language");
  await page.getByRole("button", { name: language }).click();
  await page.waitForURL("**/role");
  await page.getByRole("button", { name: role }).click();
  await page.waitForURL("**/login");
}

export async function loginByOtp(page: Page, phone: string) {
  await page.getByLabel(/Phone number/).fill(phone);
  await page.getByRole("button", { name: /Send code/ }).click();
  await page.getByLabel(/6-digit code/).fill("123456");
  await page.getByRole("button", { name: /Verify/ }).click();
}

/** Waits for the 3D farm and checks the canvas really drew a varied picture. */
export async function expectFarmRendered(page: Page) {
  await expect(page.locator('[data-scene-status="ready"]')).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(1500);
  const shot = await page.locator("canvas").screenshot();
  // A blank or single-colour canvas compresses to almost nothing.
  expect(shot.byteLength).toBeGreaterThan(30_000);
}
