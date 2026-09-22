"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AdminKnowledgeDocumentForm } from "@/features/admin/components/admin-knowledge-document-form";
import { useCreateAdminKnowledgeDocumentMutation } from "@/features/admin/hooks";
import {
  createAdminKnowledgeDocumentRequestFromValues,
  emptyAdminKnowledgeDocumentFormValues,
  validateAdminKnowledgeDocument,
} from "@/features/admin/payloads";
import {
  adminKnowledgeDocumentHref,
  adminKnowledgeHref,
} from "@/features/admin/presentation";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { buttonClassName } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";

export function AdminCreateKnowledgeDocumentPage() {
  const router = useRouter();
  const createMutation = useCreateAdminKnowledgeDocumentMutation();
  const [values, setValues] = useState(emptyAdminKnowledgeDocumentFormValues);

  const error = createMutation.isError
    ? isApiError(createMutation.error)
      ? messageForApiProblem(createMutation.error.problem)
      : "No se pudo crear el documento."
    : null;

  return (
    <Container as="main" className="py-10 md:py-16">
      <Link
        href={adminKnowledgeHref()}
        className={`${buttonClassName("ghost")} mb-4 px-0`}
      >
        Volver al listado
      </Link>
      <h1 className="text-[2rem] font-bold tracking-tight text-sf-ink md:text-[2.75rem]">
        Nuevo documento
      </h1>
      <p className="mt-2 max-w-2xl text-base text-sf-muted">
        El documento se crea en estado Recibido. Después puedes procesarlo
        desde el detalle para indexarlo.
      </p>

      {error ? (
        <div className="mt-6">
          <Alert tone="error" title="No se pudo crear el documento">
            {error}
          </Alert>
        </div>
      ) : null}

      <Card className="mt-8">
        <AdminKnowledgeDocumentForm
          values={values}
          pending={createMutation.isPending}
          onChange={setValues}
          validate={validateAdminKnowledgeDocument}
          onCancel={() => router.push(adminKnowledgeHref())}
          onSubmit={() => {
            createMutation.mutate(
              createAdminKnowledgeDocumentRequestFromValues(values),
              {
                onSuccess: (document) => {
                  router.push(adminKnowledgeDocumentHref(document.id));
                },
              },
            );
          }}
        />
      </Card>
    </Container>
  );
}
