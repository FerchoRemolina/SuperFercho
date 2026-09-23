"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import {
  validateShoppingListName,
} from "@/features/lists/api";
import { useCreateShoppingListMutation } from "@/features/lists/hooks";
import { shoppingListHref } from "@/features/lists/presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { TextField } from "@/shared/ui/text-field";

export function CreateListForm({
  onCancel,
  autofocus,
}: {
  onCancel?: () => void;
  autofocus?: boolean;
}) {
  const router = useRouter();
  const createMutation = useCreateShoppingListMutation();
  const [name, setName] = useState("");
  const [fieldError, setFieldError] = useState<string | undefined>();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const error = validateShoppingListName(name);
    setFieldError(error);
    if (error) {
      return;
    }
    try {
      const list = await createMutation.mutateAsync({ name: name.trim() });
      router.push(shoppingListHref(list.id));
    } catch {
      // Error surfaced below via mutation state.
    }
  }

  const errorMessage =
    createMutation.isError && isApiError(createMutation.error)
      ? messageForApiProblem(createMutation.error.problem)
      : createMutation.isError
        ? "No se pudo crear la lista."
        : null;

  return (
    <form className="grid gap-3" onSubmit={(event) => void handleSubmit(event)} noValidate>
      <TextField
        id="new-shopping-list-name"
        label="Nombre de la lista"
        value={name}
        error={fieldError}
        autoFocus={autofocus}
        maxLength={255}
        onChange={(event) => {
          setName(event.target.value);
          setFieldError(undefined);
        }}
      />
      {errorMessage ? (
        <Alert tone="error" title="No se pudo crear la lista">
          {errorMessage}
        </Alert>
      ) : null}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" disabled={createMutation.isPending}>
          {createMutation.isPending ? "Creando…" : "Crear lista"}
        </Button>
        {onCancel ? (
          <Button
            type="button"
            variant="secondary"
            disabled={createMutation.isPending}
            onClick={onCancel}
          >
            Cancelar
          </Button>
        ) : null}
      </div>
    </form>
  );
}
