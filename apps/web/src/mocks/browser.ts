import { setupWorker } from "msw/browser";
import { handlers } from "./handlers";

/** Starts the fake backend in the browser (NEXT_PUBLIC_USE_FAKES=true). */
export async function startBrowserFakes() {
  const worker = setupWorker(...handlers);
  await worker.start({ onUnhandledRequest: "bypass", quiet: true });
  return worker;
}
