"use client";

import { useState } from "react";
import type { UpdateCustomerProfileRequest } from "@/features/account/api";
import { ProfileForm } from "@/features/account/components/profile-form";
import { useCustomerProfileQuery, useUpdateCustomerProfileMutation } from "@/features/account/hooks";
import { isApiError } from "@/shared/errors/api-problem";
import { messageForApiProblem } from "@/shared/errors/messages";
import { Alert } from "@/shared/ui/alert";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Container } from "@/shared/ui/container";
import { PageHeader } from "@/shared/ui/page-header";
import { Skeleton } from "@/shared/ui/skeleton";

export function ProfilePageContent() {
  const profileQuery = useCustomerProfileQuery();
  const updateMutation = useUpdateCustomerProfileMutation();
  const [saved, setSaved] = useState(false);

  if (profileQuery.isPending) {
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <PageHeader
          title="Mi perfil"
          description="Consulta y actualiza tus datos personales."
          back={{ href: "/", label: "← Inicio" }}
        />
        <Card className="mt-6 grid gap-3 md:mt-8 md:p-8">
          <Skeleton className="h-11" />
          <Skeleton className="h-11" />
          <div className="grid gap-3 sm:grid-cols-2">
            <Skeleton className="h-11" />
            <Skeleton className="h-11" />
          </div>
          <Skeleton className="h-11" />
          <Skeleton className="h-14" />
        </Card>
      </Container>
    );
  }

  if (profileQuery.isError) {
    const message = isApiError(profileQuery.error)
      ? messageForApiProblem(profileQuery.error.problem)
      : "No se pudo cargar tu perfil.";
    return (
      <Container as="main" width="wide" className="py-8 md:py-12">
        <PageHeader
          title="Mi perfil"
          description="Consulta y actualiza tus datos personales."
          back={{ href: "/", label: "← Inicio" }}
        />
        <div className="mt-6 grid gap-4 md:mt-8">
          <Alert tone="error" title="No se pudo cargar tu perfil">
            {message}
          </Alert>
          <Button
            type="button"
            variant="secondary"
            onClick={() => profileQuery.refetch()}
          >
            Reintentar
          </Button>
        </div>
      </Container>
    );
  }

  const profile = profileQuery.data;

  return (
    <Container as="main" width="wide" className="py-8 md:py-12">
      <PageHeader
        title="Mi perfil"
        description="Consulta y actualiza tus datos personales."
        back={{ href: "/", label: "← Inicio" }}
      />
      <Card className="mt-6 grid gap-4 md:mt-8 md:p-8">
        <ProfileForm
          profile={profile}
          pending={updateMutation.isPending}
          saved={saved}
          error={updateMutation.error}
          onSubmit={(body: UpdateCustomerProfileRequest) => {
            setSaved(false);
            updateMutation.mutate(body, {
              onSuccess: () => setSaved(true),
            });
          }}
        />
      </Card>
    </Container>
  );
}
