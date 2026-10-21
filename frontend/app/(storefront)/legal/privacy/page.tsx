import {
  LegalDocument,
  LegalList,
  LegalParagraph,
  LegalSection,
} from "@/features/legal/components/legal-document";

export const metadata = {
  title: "Política de privacidad | SuperFercho",
};

export default function PrivacyPage() {
  return (
    <LegalDocument
      title="Política de privacidad"
      updatedAt="17 de octubre de 2026"
      intro="Esta política explica, en lenguaje sencillo, qué información personal tratamos cuando usas la tienda en línea de SuperFercho, para qué la utilizamos y qué opciones tienes. Es una versión inicial de ejemplo: antes de su publicación definitiva debe ser revisada y completada con la información del responsable del tratamiento."
      other={{ href: "/legal/terms", label: "Ver Términos y condiciones" }}
    >
      <LegalSection title="1. Información que solicitamos">
        <LegalParagraph>
          Al crear tu cuenta solicitamos: tipo y número de documento, nombres,
          apellido, correo electrónico, teléfono y una contraseña. Al usar la
          tienda se generan además direcciones de entrega, pedidos y registros
          de pago asociados a tu cuenta.
        </LegalParagraph>
      </LegalSection>

      <LegalSection title="2. Para qué utilizamos la información">
        <LegalList>
          <li>Crear y mantener tu cuenta de cliente y autenticarte.</li>
          <li>Gestionar tus direcciones de entrega y tus listas de compras.</li>
          <li>Procesar tus pedidos, prepararlos y entregarlos en la dirección seleccionada.</li>
          <li>Registrar el estado de tu pago y de tu pedido.</li>
          <li>Atender consultas a través del asistente Fercho.</li>
        </LegalList>
      </LegalSection>

      <LegalSection title="3. Asistente Fercho y proveedores externos">
        <LegalParagraph>
          El asistente Fercho transmite el contenido de tus consultas a un
          proveedor externo (OpenAI) para generar sus respuestas. No envíes
          información sensible que no sea necesaria para tu consulta.
        </LegalParagraph>
      </LegalSection>

      <LegalSection title="4. Seguridad">
        <LegalParagraph>
          Las contraseñas se almacenan cifradas (hash) y el acceso a tu cuenta
          y a tu información se realiza mediante sesiones con controles de
          acceso por usuario. Trabajamos para proteger tu información con
          medidas de seguridad adecuadas; ningún sistema puede garantizar
          seguridad absoluta.
        </LegalParagraph>
      </LegalSection>

      <LegalSection title="5. Tus opciones">
        <LegalParagraph>
          Puedes consultar y corregir la información de tu cuenta y de tus
          direcciones desde las secciones correspondientes de la tienda, y
          eliminar tu cuenta a través del mecanismo disponible para ello, sujeto
          a las obligaciones de conservación aplicables. Para consultas o
          solicitudes sobre el tratamiento de tus datos puedes usar el canal de
          contacto que SuperFercho defina para ello.
        </LegalParagraph>
      </LegalSection>

      <LegalSection title="6. Conservación">
        <LegalParagraph>
          Conservamos tu información mientras tu cuenta esté activa y durante
          los plazos necesarios para cumplir las obligaciones asociadas a tus
          pedidos. Aún no contamos con una política general de plazos de
          conservación publicada.
        </LegalParagraph>
      </LegalSection>
    </LegalDocument>
  );
}
