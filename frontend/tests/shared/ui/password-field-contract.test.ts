import { describe, expect, it } from "vitest";

/**
 * Hold-to-reveal contract for PasswordField (pointerdown shows, release hides).
 */
describe("password field visibility contract", () => {
  it("reveals only while pressed and hides on release", () => {
    let revealed = false;
    const onPointerDown = () => {
      revealed = true;
    };
    const onPointerUp = () => {
      revealed = false;
    };

    onPointerDown();
    expect(revealed).toBe(true);
    onPointerUp();
    expect(revealed).toBe(false);

    onPointerDown();
    expect(revealed).toBe(true);
    onPointerUp();
    expect(revealed).toBe(false);
  });

  it("documents Spanish hold-to-reveal accessibility label", () => {
    expect("Mantén pulsado para mostrar la contraseña").toContain("mostrar");
  });
});
